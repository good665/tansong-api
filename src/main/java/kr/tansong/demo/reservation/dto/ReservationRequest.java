package kr.tansong.demo.reservation.dto;

public class ReservationRequest {

    private Long userId;
    private Long facilityId;
    private String scheduledAt;
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

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
