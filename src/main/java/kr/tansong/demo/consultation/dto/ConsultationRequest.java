package kr.tansong.demo.consultation.dto;

public class ConsultationRequest {

    private Long userId;
    private Long facilityId;
    private String preferredAt;
    private String consultationMethod;
    private String message;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getPreferredAt() {
        return preferredAt;
    }

    public void setPreferredAt(String preferredAt) {
        this.preferredAt = preferredAt;
    }

    public String getConsultationMethod() {
        return consultationMethod;
    }

    public void setConsultationMethod(String consultationMethod) {
        this.consultationMethod = consultationMethod;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
