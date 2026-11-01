package kr.tansong.demo.publicdata;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 공공데이터포털 API 호출 (응답은 모두 XML):
 * - 국민건강보험공단 장기요양기관 검색 / 시설별 상세조회(일반현황)
 * - 행정안전부 법정동코드 (상세조회가 주소를 코드로만 주므로 지역명으로 바꾸는 데 사용)
 * 인증키는 포털 계정 공통 키(Decoding 키). 요청 URL 에 키가 들어가므로 URL 은 로그에 남기지 않습니다.
 */
@Component
public class LtcApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    public LtcApiClient(RestClient.Builder builder,
                        @Value("${app.public-data.base-url}") String baseUrl,
                        @Value("${app.public-data.service-key:}") String serviceKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        this.restClient = builder.requestFactory(factory).build();
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
    }

    public record Page(List<Map<String, String>> items, int totalCount) {}

    /** 시도별 장기요양기관 목록 (한 기관이 급여종류별로 여러 행일 수 있음) */
    public Page search(String siDoCd, int pageNo, int numOfRows) {
        return call("/B550928/searchLtcInsttService02/getLtcInsttSeachList02",
                "siDoCd=" + siDoCd + "&pageNo=" + pageNo + "&numOfRows=" + numOfRows);
    }

    /** 일반현황: 주소 코드(법정동/도로명), 상세주소, 전화번호 */
    public Map<String, String> generalInfo(String longTermAdminSym, String adminPttnCd) {
        List<Map<String, String>> items = call("/B550928/getLtcInsttDetailInfoService02/getGeneralSttusDetailInfoItem02",
                "longTermAdminSym=" + longTermAdminSym + "&adminPttnCd=" + adminPttnCd).items();
        return items.isEmpty() ? Map.of() : items.get(0);
    }

    /** 법정동코드 목록 (예: "서울특별시" → 그 아래 모든 시군구/읍면동/리) */
    public Page regionCodes(String regionName, int pageNo, int numOfRows) {
        return call("/1741000/StanReginCd/getStanReginCdList",
                "type=xml&pageNo=" + pageNo + "&numOfRows=" + numOfRows
                        + "&locatadd_nm=" + URLEncoder.encode(regionName, StandardCharsets.UTF_8));
    }

    private Page call(String path, String query) {
        if (serviceKey.isEmpty()) {
            throw new IllegalStateException("공공데이터포털 인증키가 설정되지 않았습니다. (DATA_GO_KR_SERVICE_KEY 또는 NTS_SERVICE_KEY)");
        }

        URI uri = URI.create(baseUrl + path + "?" + query
                + "&serviceKey=" + URLEncoder.encode(serviceKey, StandardCharsets.UTF_8));
        byte[] body = restClient.get().uri(uri).retrieve().body(byte[].class);
        return parse(body == null ? new byte[0] : body);
    }

    /** item 요소들을 {자식태그: 값} 으로. 포털 공통 오류(인증키 미등록 등)는 메시지와 함께 예외 */
    static Page parse(byte[] xml) {
        Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // XXE 방지
            doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            String head = new String(xml, 0, Math.min(xml.length, 200), StandardCharsets.UTF_8);
            throw new IllegalStateException("공공데이터 응답을 해석하지 못했습니다: " + head);
        }

        String resultCode = text(doc, "resultCode");
        String authMsg = text(doc, "returnAuthMsg");
        boolean ok = resultCode == null || resultCode.equals("00") || resultCode.equals("INFO-0") || resultCode.equals("0");
        if (authMsg != null || !ok) {
            String msg = authMsg != null ? authMsg : resultCode + " " + text(doc, "resultMsg");
            if (!"INFO-200".equals(resultCode)) { // INFO-200 = 결과 없음
                throw new IllegalStateException("공공데이터 API 오류: " + msg);
            }
        }

        List<Map<String, String>> items = new ArrayList<>();
        // 장기요양 API 는 <item>, 법정동코드 API 는 <row>
        for (String tag : new String[] {"item", "row"}) {
            NodeList nodes = doc.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Map<String, String> row = new LinkedHashMap<>();
                NodeList children = nodes.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node c = children.item(j);
                    if (c instanceof Element) {
                        row.put(c.getNodeName(), c.getTextContent().trim());
                    }
                }
                items.add(row);
            }
        }

        String total = text(doc, "totalCount");
        return new Page(items, total == null || total.isBlank() ? items.size() : Integer.parseInt(total));
    }

    private static String text(Document doc, String tag) {
        NodeList n = doc.getElementsByTagName(tag);
        return n.getLength() == 0 ? null : n.item(0).getTextContent().trim();
    }
}
