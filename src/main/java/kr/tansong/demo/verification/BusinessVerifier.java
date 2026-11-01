package kr.tansong.demo.verification;

import java.time.LocalDate;

/** 사업자 진위확인 창구 (구현체: NtsBusinessVerifier). 테스트에서 국세청을 부르지 않도록 인터페이스로 분리 */
public interface BusinessVerifier {

    /** businessNumber 는 숫자 10자리. 네트워크 오류 등은 예외 대신 UNAVAILABLE 로 돌려줍니다. */
    VerificationResult verify(String businessNumber, String representativeName, LocalDate startDate);
}
