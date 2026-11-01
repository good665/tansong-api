package kr.tansong.demo.subscription.domain;

import java.time.LocalDateTime;

/** subscriptions 테이블 한 행 (시설당 하나).
 *  status: ACTIVE = 자동 연장 켜짐, CANCELED = 자동 연장 꺼짐(기간 끝까지는 노출), EXPIRED = 연장 결제 실패 등으로 종료.
 *  실제 상단 노출 여부는 status 가 아니라 currentPeriodEnd 가 아직 지나지 않았는지로 판단합니다. */
public class Subscription {

    private Long facilityId;
    private String plan;
    private String status;
    private LocalDateTime currentPeriodEnd;

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }
}
