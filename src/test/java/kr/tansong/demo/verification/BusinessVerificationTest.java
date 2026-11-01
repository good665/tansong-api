package kr.tansong.demo.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 사업자번호 형식 검사와 국세청 진위확인 응답 해석 (실제 국세청은 호출하지 않음) */
class BusinessVerificationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode json(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }

    @Test
    void 체크섬이_맞는_번호만_유효하다() {
        assertThat(BusinessNumbers.isValid(BusinessNumbers.normalize("124-81-00998"))).isTrue();
        assertThat(BusinessNumbers.isValid(BusinessNumbers.normalize("123-45-67891"))).isTrue();
        assertThat(BusinessNumbers.isValid("1234567890")).isFalse();
        assertThat(BusinessNumbers.isValid("12345")).isFalse();
        assertThat(BusinessNumbers.isValid(null)).isFalse();
    }

    @Test
    void 개업일자는_두가지_형식을_받고_미래날짜는_거절한다() {
        LocalDate today = LocalDate.of(2026, 10, 30);
        assertThat(BusinessNumbers.parseStartDate("2020-01-31", today)).isEqualTo(LocalDate.of(2020, 1, 31));
        assertThat(BusinessNumbers.parseStartDate("20200131", today)).isEqualTo(LocalDate.of(2020, 1, 31));
        assertThatThrownBy(() -> BusinessNumbers.parseStartDate("2026-10-31", today))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BusinessNumbers.parseStartDate("2020-02-30", today))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BusinessNumbers.parseStartDate(null, today))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 일치하고_계속사업자면_VERIFIED() throws Exception {
        VerificationResult result = NtsBusinessVerifier.parse(json("""
            {"status_code":"OK","request_cnt":1,"valid_cnt":1,
             "data":[{"b_no":"1234567891","valid":"01",
                      "status":{"b_no":"1234567891","b_stt":"계속사업자","b_stt_cd":"01"}}]}
            """));
        assertThat(result.status()).isEqualTo(VerificationResult.Status.VERIFIED);
        assertThat(result.ntsStatusCode()).isEqualTo("01");
    }

    @Test
    void 불일치면_MISMATCH() throws Exception {
        VerificationResult result = NtsBusinessVerifier.parse(json("""
            {"status_code":"OK","request_cnt":1,"valid_cnt":0,
             "data":[{"b_no":"1234567891","valid":"02","valid_msg":"확인할 수 없습니다."}]}
            """));
        assertThat(result.status()).isEqualTo(VerificationResult.Status.MISMATCH);
    }

    @Test
    void 일치해도_휴업이나_폐업이면_CLOSED() throws Exception {
        VerificationResult closed = NtsBusinessVerifier.parse(json("""
            {"data":[{"valid":"01","status":{"b_stt":"폐업자","b_stt_cd":"03"}}]}
            """));
        VerificationResult suspended = NtsBusinessVerifier.parse(json("""
            {"data":[{"valid":"01","status":{"b_stt":"휴업자","b_stt_cd":"02"}}]}
            """));
        assertThat(closed.status()).isEqualTo(VerificationResult.Status.CLOSED);
        assertThat(closed.message()).contains("폐업");
        assertThat(suspended.message()).contains("휴업");
    }

    @Test
    void 해석할수_없는_응답은_UNAVAILABLE() throws Exception {
        assertThat(NtsBusinessVerifier.parse(json("{\"status_code\":\"BAD_JSON_REQUEST\"}")).status())
                .isEqualTo(VerificationResult.Status.UNAVAILABLE);
        assertThat(NtsBusinessVerifier.parse(null).status())
                .isEqualTo(VerificationResult.Status.UNAVAILABLE);
    }
}
