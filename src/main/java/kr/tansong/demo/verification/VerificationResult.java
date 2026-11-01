package kr.tansong.demo.verification;

/** 국세청 진위확인 결과. status 이름이 그대로 business_verifications.status 에 저장됩니다. */
public record VerificationResult(Status status, String ntsStatusCode, String message) {

    public enum Status {
        VERIFIED,     // 번호·대표자·개업일자 일치 + 계속사업자
        MISMATCH,     // 입력한 정보가 국세청 등록 정보와 다름
        CLOSED,       // 일치하지만 휴업/폐업 상태
        UNAVAILABLE   // API 장애·인증키 미설정 등으로 확인하지 못함 (가입은 받고 관리자 승인 때 다시 확인)
    }

    public static VerificationResult verified(String ntsStatusCode) {
        return new VerificationResult(Status.VERIFIED, ntsStatusCode, "국세청 확인 완료");
    }

    public static VerificationResult mismatch() {
        return new VerificationResult(Status.MISMATCH, null, "국세청 등록 정보와 일치하지 않습니다.");
    }

    public static VerificationResult closed(String ntsStatusCode) {
        String label = "02".equals(ntsStatusCode) ? "휴업" : "폐업";
        return new VerificationResult(Status.CLOSED, ntsStatusCode, label + " 상태인 사업자입니다.");
    }

    public static VerificationResult unavailable(String message) {
        return new VerificationResult(Status.UNAVAILABLE, null, message);
    }
}
