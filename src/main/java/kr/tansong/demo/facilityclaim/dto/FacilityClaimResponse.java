package kr.tansong.demo.facilityclaim.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 시설 소유권 신청. 신청자·사업자 정보는 관리자 목록에서만 채워짐 */
public class FacilityClaimResponse {

    public Long id;
    public Long facilityId;
    public String facilityName;
    public String facilityAddress;
    public String facilityType;
    public Long organizationId;
    public String organizationName;
    public String businessRegistrationNumber;
    public String verificationStatus;
    public String requesterName;
    public String requesterPhone;
    public String requesterEmail;
    public String message;
    public String status; // PENDING, APPROVED, REJECTED
    public String rejectReason;
    public String createdAt;
    public String decidedAt;

    // 저장 위치는 응답에 내보내지 않음
    @JsonIgnore
    public String documentKey;

    @JsonProperty("hasDocument")
    public boolean hasDocument() {
        return documentKey != null;
    }
}
