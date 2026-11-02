package kr.tansong.demo.publicdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** 공공데이터 응답 해석과 시설 변환 규칙 */
class LtcImportServiceTest {

    @Test
    void 일반현황_XML을_해석한다() {
        String xml = """
            <response><header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
            <body><item><adminNm>번동데이케어센터</adminNm><adminPttnCd>B03</adminPttnCd><BDongCd>102</BDongCd>
            <locTelNo_1>02</locTelNo_1><locTelNo_2>980</locTelNo_2><locTelNo_3>3004</locTelNo_3>
            <riCd>00</riCd><siDoCd>11</siDoCd><siGunGuCd>305</siGunGuCd></item></body></response>""";

        LtcApiClient.Page page = LtcApiClient.parse(xml.getBytes(StandardCharsets.UTF_8));
        Map<String, String> g = page.items().get(0);

        assertThat(g.get("adminNm")).isEqualTo("번동데이케어센터");
        assertThat(LtcImportService.phone(g)).isEqualTo("02-980-3004");
        assertThat(LtcImportService.address(g, Map.of("1130510200", "서울특별시 강북구 번동")))
                .isEqualTo("서울특별시 강북구 번동");
    }

    @Test
    void 인증키_오류는_메시지와_함께_예외() {
        String xml = """
            <OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>
            <returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg><returnReasonCode>30</returnReasonCode>
            </cmmMsgHeader></OpenAPI_ServiceResponse>""";

        assertThatThrownBy(() -> LtcApiClient.parse(xml.getBytes(StandardCharsets.UTF_8)))
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    void 기관유형을_우리_시설종류로_바꾸고_입소시설을_우선한다() {
        var home = LtcImportService.toInstitution(Map.of("longTermAdminSym", "1", "adminPttnCd", "C01", "adminNm", "A"));
        var nursing = LtcImportService.toInstitution(Map.of("longTermAdminSym", "1", "adminPttnCd", "A03", "adminNm", "A"));

        assertThat(home.facilityType()).isEqualTo("HOME_CARE");
        assertThat(LtcImportService.preferred(home, nursing).facilityType()).isEqualTo("NURSING_HOME");
        assertThat(LtcImportService.toInstitution(Map.of("longTermAdminSym", "2", "adminPttnCd", "C06"))).isNull(); // 복지용구 제외
    }

    @Test
    void 법정동을_못찾으면_시군구까지만_전화가_없으면_null() {
        Map<String, String> g = Map.of("siDoCd", "41", "siGunGuCd", "590", "BDongCd", "999", "riCd", "00");
        assertThat(LtcImportService.address(g, Map.of("4159000000", "경기도 화성시"))).isEqualTo("경기도 화성시");
        assertThat(LtcImportService.phone(Map.of())).isNull();
    }

    @Test
    void 도로명_주소가_있으면_공백을_정리해_우선_사용한다() {
        Map<String, String> g = Map.of("adminAddr", "경기도 수원시 장안구 수일로  33 (파장동)", "siDoCd", "41", "siGunGuCd", "111");
        assertThat(LtcImportService.address(g, Map.of("4111100000", "경기도 수원시 장안구")))
                .isEqualTo("경기도 수원시 장안구 수일로 33 (파장동)");
    }

    @Test
    void 목록에서_사라진_기관만_같은_지역에서_고른다() {
        List<String> imported = new java.util.ArrayList<>(Collections.nCopies(30, "11100000000"));
        imported.add("21100000001"); // 서울, 목록에 없음
        imported.add("14100000002"); // 경기, 목록에 없지만 다른 지역

        assertThat(LtcImportService.missing(imported, Set.of("11100000000"), "11")).containsExactly("21100000001");
    }

    @Test
    void 사라진_기관이_5퍼센트를_넘으면_지우지_않는다() {
        assertThat(LtcImportService.missing(List.of("11100000000", "21100000001"), Set.of(), "11")).isEmpty();
    }
}
