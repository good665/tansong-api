package kr.tansong.demo.facility.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.facility.mapper.FacilitySearchMapper;

@Service
public class FacilityService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final int MAX_PAGE = 10_000;
    private static final int MAX_KEYWORD_LENGTH = 50;

    private final FacilityMapper facilityMapper;
    private final FacilitySearchMapper facilitySearchMapper;
    private final FacilityImageMapper facilityImageMapper;

    public FacilityService(FacilityMapper facilityMapper,
                           FacilitySearchMapper facilitySearchMapper,
                           FacilityImageMapper facilityImageMapper) {
        this.facilityMapper = facilityMapper;
        this.facilitySearchMapper = facilitySearchMapper;
        this.facilityImageMapper = facilityImageMapper;
    }

    public List<FacilityResponse> findAll(String type, String keyword) {
        List<FacilityResponse> items = facilityMapper.findAll(type, keyword);
        items.forEach(this::attachPhotos);
        return items;
    }

    public FacilityResponse getFacilityById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("시설 ID가 올바르지 않습니다.");
        }
        FacilityResponse facility = facilityMapper.findById(id);
        attachPhotos(facility);
        return facility;
    }

    /** 서버 쪽 검색 + 페이징 (page 는 0부터 시작).
     *  seed: 광고 시설끼리의 무작위 순서를 정하는 값 — 앱이 검색할 때마다 새로 만들고 다음 페이지에도 같은 값을 보냄.
     *  없으면 요청마다 새로 섞습니다. */
    public PageResponse<FacilityResponse> search(String type, String keyword, Integer seed, int page, int size) {

        page = Math.max(0, Math.min(page, MAX_PAGE));
        size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        String normalizedType =
                (type == null || type.isBlank() || "ALL".equals(type)) ? null : type.trim();

        String normalizedKeyword = null;
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            if (k.length() > MAX_KEYWORD_LENGTH) {
                k = k.substring(0, MAX_KEYWORD_LENGTH);
            }
            normalizedKeyword = escapeLike(k);
        }

        String alias = normalizedKeyword == null ? null : regionAlias(normalizedKeyword);
        long total = facilitySearchMapper.count(normalizedType, normalizedKeyword, alias);
        List<FacilityResponse> items =
                facilitySearchMapper.search(normalizedType, normalizedKeyword, alias,
                        seed != null ? seed : ThreadLocalRandom.current().nextInt(), size, page * size);
        items.forEach(this::attachPhotos);

        return new PageResponse<>(items, page, size, total);
    }

    // 시도 이름 한글 ↔ 영문: "서울"로 찾아도 주소가 "Seoul"인 시설이, "seoul"로 찾아도 "서울" 시설이 나오게
    private static final Map<String, String> REGION_ALIASES = buildRegionAliases(
            "서울", "Seoul", "부산", "Busan", "대구", "Daegu", "인천", "Incheon", "광주", "Gwangju",
            "대전", "Daejeon", "울산", "Ulsan", "세종", "Sejong", "경기", "Gyeonggi", "강원", "Gangwon",
            "충북", "Chungbuk", "충남", "Chungnam", "전북", "Jeonbuk", "전남", "Jeonnam",
            "경북", "Gyeongbuk", "경남", "Gyeongnam", "제주", "Jeju");

    private static Map<String, String> buildRegionAliases(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
            map.put(pairs[i + 1].toLowerCase(Locale.ROOT), pairs[i]);
        }
        return Map.copyOf(map);
    }

    /** 검색어가 시도 이름(서울, 서울특별시, seoul …)이면 반대 언어 이름, 아니면 null */
    static String regionAlias(String keyword) {
        String k = keyword.toLowerCase(Locale.ROOT).replaceAll("(특별시|광역시|특별자치시|특별자치도|도)$", "");
        return REGION_ALIASES.get(k);
    }

    private void attachPhotos(FacilityResponse facility) {
        if (facility == null) {
            return;
        }
        facility.setPhotos(facilityImageMapper.findImageUrlsByFacilityId(facility.getId()));
    }

    // 사용자가 입력한 % _ 가 와일드카드로 동작하지 않게 이스케이프
    private String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}