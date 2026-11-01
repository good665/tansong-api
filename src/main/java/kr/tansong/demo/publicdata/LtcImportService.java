package kr.tansong.demo.publicdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

import kr.tansong.demo.publicdata.mapper.PublicFacilityMapper;

/**
 * 국민건강보험공단 장기요양기관 → 우리 시설(facilities)로 들여오기.
 * 기관마다 상세조회를 1번 호출하므로(하루 10,000건 한도) limit 만큼씩 나눠 실행하고,
 * 이미 들여온 기관코드는 건너뛰어 여러 번 실행해도 중복되지 않습니다.
 */
@Service
public class LtcImportService {

    private static final Logger log = LoggerFactory.getLogger(LtcImportService.class);

    static final String SOURCE = "NHIS_LTC";
    static final String ORG_NAME = "국민건강보험공단 장기요양기관 (공공데이터)";

    // 지원 지역 (법정동 시도코드 → 법정동코드 API 검색어). 지역을 늘릴 때 여기에 추가
    static final Map<String, String> REGIONS = Map.of("11", "서울특별시", "41", "경기도");

    // 기관유형코드(활용가이드 "기관유형코드 정의") → 우리 시설 종류. 단기보호·복지용구·기타는 들여오지 않음
    static final Map<String, String> TYPE_MAP = Map.ofEntries(
            Map.entry("A01", "NURSING_HOME"), Map.entry("A02", "NURSING_HOME"), Map.entry("A03", "NURSING_HOME"),
            Map.entry("A04", "NURSING_HOME"), Map.entry("A05", "NURSING_HOME"),
            Map.entry("B03", "DAY_CARE"), Map.entry("C03", "DAY_CARE"),
            Map.entry("B01", "HOME_CARE"), Map.entry("B02", "HOME_CARE"), Map.entry("B05", "HOME_CARE"),
            Map.entry("C01", "HOME_CARE"), Map.entry("C02", "HOME_CARE"), Map.entry("C05", "HOME_CARE"));

    private static final List<String> TYPE_PRIORITY = List.of("NURSING_HOME", "DAY_CARE", "HOME_CARE");

    static final Map<String, String> TYPE_NAMES = Map.ofEntries(
            Map.entry("A01", "노인요양시설"), Map.entry("A02", "노인전문요양시설"), Map.entry("A03", "노인요양시설"),
            Map.entry("A04", "노인요양공동생활가정"), Map.entry("A05", "노인요양시설(단기보호 전환)"),
            Map.entry("B03", "주야간보호"), Map.entry("C03", "주야간보호"),
            Map.entry("B01", "방문요양"), Map.entry("C01", "방문요양"), Map.entry("B02", "방문목욕"),
            Map.entry("C02", "방문목욕"), Map.entry("B05", "방문간호"), Map.entry("C05", "방문간호"));

    private static final int PAGE_SIZE = 1000;
    // 포털 초당 요청 제한(429 LIMITED_NUMBER_OF_SERVICE_REQUESTS_PER_SECOND) 회피: 상세조회 사이 간격과 재시도
    private static final long CALL_INTERVAL_MS = 250;
    private static final long RETRY_WAIT_MS = 3_000;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final LtcApiClient client;
    private final PublicFacilityMapper mapper;
    private final TransactionTemplate tx;

    public LtcImportService(LtcApiClient client, PublicFacilityMapper mapper, TransactionTemplate tx) {
        this.client = client;
        this.mapper = mapper;
        this.tx = tx;
    }

    public record ImportResult(String region, int institutions, int alreadyImported,
                               int imported, int failed, int remaining, String firstError) {}

    /** 한 기관 = 기관코드 + 우리 시설 종류 + 그 종류의 대표 기관유형코드 */
    record Institution(String sym, String name, String facilityType, String pttnCd, String regionCd) {}

    public ImportResult importRegion(String siDoCd, int limit) {
        String regionName = REGIONS.get(siDoCd);
        if (regionName == null) {
            throw new IllegalArgumentException("지원하지 않는 지역입니다. 지원: " + REGIONS);
        }
        if (limit < 1 || limit > 2000) {
            throw new IllegalArgumentException("한 번에 들여올 수는 1~2000 사이로 지정해 주세요.");
        }

        Map<String, Institution> institutions;
        try {
            institutions = fetchInstitutions(siDoCd);
        } catch (RuntimeException e) {
            // 관리자에게 원인(인증키 미등록 등)을 그대로 보여주기 위해 500 대신 결과로 반환
            return new ImportResult(regionName, 0, 0, 0, 0, 0, "목록 조회 실패: " + e.getMessage());
        }
        Set<String> existing = new HashSet<>(mapper.findExternalIds(SOURCE));
        List<Institution> todo = institutions.values().stream().filter(i -> !existing.contains(i.sym())).toList();

        int imported = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        String firstError = null;
        Long orgId = null;
        Map<String, String> regionNames = null;

        for (Institution inst : todo.subList(0, Math.min(limit, todo.size()))) {
            try {
                if (orgId == null) {
                    orgId = organizationId();
                    regionNames = fetchRegionNames(regionName);
                }
                Map<String, String> g = generalInfoThrottled(inst);
                saveOne(orgId, inst, g, regionNames, regionName);
                imported++;
                consecutiveFailures = 0;
            } catch (RuntimeException e) {
                failed++;
                consecutiveFailures++;
                if (firstError == null) {
                    firstError = e.getMessage();
                }
                log.warn("장기요양기관 {} 들여오기 실패: {}", inst.sym(), e.getMessage());
                // 인증키·일일 한도 문제면 나머지도 실패해 호출 한도만 낭비하므로 중단
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    break;
                }
            }
        }

        int alreadyImported = institutions.size() - todo.size();
        return new ImportResult(regionName, institutions.size(), alreadyImported, imported, failed,
                todo.size() - imported, firstError);
    }

    private Map<String, String> generalInfoThrottled(Institution inst) {
        sleep(CALL_INTERVAL_MS);
        try {
            return client.generalInfo(inst.sym(), inst.pttnCd());
        } catch (HttpClientErrorException.TooManyRequests e) {
            sleep(RETRY_WAIT_MS);
            return client.generalInfo(inst.sym(), inst.pttnCd());
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("가져오기가 중단되었습니다.");
        }
    }

    private Map<String, Institution> fetchInstitutions(String siDoCd) {
        Map<String, Institution> bySym = new LinkedHashMap<>();
        int page = 1;
        while (true) {
            LtcApiClient.Page p = client.search(siDoCd, page, PAGE_SIZE);
            for (Map<String, String> row : p.items()) {
                Institution candidate = toInstitution(row);
                if (candidate != null) {
                    bySym.merge(candidate.sym(), candidate, LtcImportService::preferred);
                }
            }
            if (p.items().isEmpty() || page * PAGE_SIZE >= p.totalCount()) {
                return bySym;
            }
            page++;
        }
    }

    static Institution toInstitution(Map<String, String> row) {
        String sym = row.get("longTermAdminSym");
        String type = TYPE_MAP.get(row.get("adminPttnCd"));
        if (sym == null || sym.isBlank() || type == null) {
            return null;
        }
        // 상세조회에 주소가 없을 때 쓸 시군구 코드(10자리 법정동코드의 시군구 단위)
        String regionCd = row.getOrDefault("siDoCd", "") + row.getOrDefault("siGunGuCd", "") + "00000";
        return new Institution(sym, row.getOrDefault("adminNm", "").trim(), type, row.get("adminPttnCd"), regionCd);
    }

    /** 한 기관이 여러 급여종류면 입소시설 > 주야간보호 > 방문 순으로 대표 종류를 정함 */
    static Institution preferred(Institution a, Institution b) {
        return TYPE_PRIORITY.indexOf(b.facilityType()) < TYPE_PRIORITY.indexOf(a.facilityType()) ? b : a;
    }

    private void saveOne(Long orgId, Institution inst, Map<String, String> g,
                         Map<String, String> regionNames, String regionName) {
        Map<String, Object> f = new HashMap<>();
        f.put("organizationId", orgId);
        f.put("facilityType", inst.facilityType());
        f.put("name", inst.name().isBlank() ? g.getOrDefault("adminNm", "이름 미상") : inst.name());
        String address = address(g, regionNames);
        // 일부 기관은 상세조회(일반현황)가 비어 있음 → 목록의 시군구까지라도 표시 (address 는 NOT NULL)
        f.put("address", address != null ? address : regionNames.getOrDefault(inst.regionCd(), regionName));
        f.put("phone", phone(g));
        f.put("introduction", "정보 출처: 국민건강보험공단 장기요양기관 공공데이터\n기관 유형: "
                + TYPE_NAMES.getOrDefault(inst.pttnCd(), inst.pttnCd()) + date(" · 장기요양기관 지정일: ", g.get("longTermPeribRgtDt")));

        tx.executeWithoutResult(status -> {
            mapper.insertFacility(f);
            mapper.insertSource(((Number) f.get("id")).longValue(), SOURCE, inst.sym());
        });
    }

    /**
     * shortcut: 상세조회가 도로명은 코드(roadNmCd)로만 줘서 "시도 시군구 법정동 + 상세주소" 까지만 표시 —
     * 정확한 도로명 주소가 필요해지면 도로명주소 API(juso.go.kr)로 roadNmCd+건물번호를 변환
     */
    static String address(Map<String, String> g, Map<String, String> regionNames) {
        String sido = g.getOrDefault("siDoCd", "");
        String sgg = g.getOrDefault("siGunGuCd", "");
        String dong = g.getOrDefault("BDongCd", "");
        String ri = g.getOrDefault("riCd", "00");
        String name = regionNames.get(sido + sgg + dong + (ri.isBlank() ? "00" : ri));
        if (name == null) {
            name = regionNames.get(sido + sgg + dong + "00");
        }
        if (name == null) {
            name = regionNames.getOrDefault(sido + sgg + "00000", "");
        }
        String detail = g.getOrDefault("detailAddr", "").trim();
        String result = (name + " " + detail).trim();
        return result.isEmpty() ? null : result;
    }

    static String phone(Map<String, String> g) {
        String p = String.join("-", List.of(
                g.getOrDefault("locTelNo_1", ""), g.getOrDefault("locTelNo_2", ""), g.getOrDefault("locTelNo_3", "")))
                .replaceAll("^-+|-+$", "").replaceAll("-{2,}", "-");
        return p.isBlank() ? null : p;
    }

    private static String date(String label, String yyyymmdd) {
        if (yyyymmdd == null || !yyyymmdd.matches("\\d{8}")) {
            return "";
        }
        return label + yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6);
    }

    private Long organizationId() {
        Long id = mapper.findOrganizationIdByName(ORG_NAME);
        if (id != null) {
            return id;
        }
        Map<String, Object> p = new HashMap<>(Map.of("name", ORG_NAME, "type", "공공데이터"));
        mapper.insertOrganization(p);
        return ((Number) p.get("id")).longValue();
    }

    /** 법정동코드(10자리) → "서울특별시 강북구 번동" */
    private Map<String, String> fetchRegionNames(String regionName) {
        Map<String, String> names = new HashMap<>();
        int page = 1;
        while (true) {
            LtcApiClient.Page p = client.regionCodes(regionName, page, PAGE_SIZE);
            for (Map<String, String> row : p.items()) {
                if (row.get("region_cd") != null && row.get("locatadd_nm") != null) {
                    names.put(row.get("region_cd"), row.get("locatadd_nm"));
                }
            }
            if (p.items().isEmpty() || page * PAGE_SIZE >= p.totalCount()) {
                return names;
            }
            page++;
        }
    }
}
