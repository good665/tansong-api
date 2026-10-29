package kr.tansong.demo.admin.dto;

public class AdminConsultationResponse {

    private Long id;
    private Long facilityId;
    private String facilityName;
    private Long userId;
    private String userName;
    private String userEmail;
    private String preferredAt;
    private String consultationMethod;
    private String message;
    private String status;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
