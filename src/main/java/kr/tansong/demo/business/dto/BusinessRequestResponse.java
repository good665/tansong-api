package kr.tansong.demo.business.dto;

/** 사업자가 보는 상담·방문 예약 신청. 연락에 필요한 이름과 전화번호만 포함 (이메일 제외) */
public class BusinessRequestResponse {

    public Long id;
    public Long facilityId;
    public String facilityName;
    public String userName;
    public String userPhone;
    public String requestedAt;        // 희망 상담 일시 또는 방문 일시
    public String consultationMethod; // 상담만 있음
    public String message;
    public String status;
    public String createdAt;
}
