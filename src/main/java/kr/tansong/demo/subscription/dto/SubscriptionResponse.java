package kr.tansong.demo.subscription.dto;

public class SubscriptionResponse {

    private Long facilityId;
    private String plan;
    private String status;
    private String currentPeriodEnd;
    // 지금 상단 노출(광고) 중인지 — currentPeriodEnd 가 아직 지나지 않았는지로 판단
    private boolean active;
    // 기간이 끝날 때 자동으로 다음 달 결제가 되는지 (status = ACTIVE)
    private boolean autoRenew;
    // 상단 노출되는 마지막 날 (yyyy-MM-dd) — 다음날 00:00 에 내려갑니다. 노출 중이 아니면 null
    private String premiumUntil;
    private int monthlyPrice;
    // 실제 PG(토스) 모드면 결제 전에 카드 등록이 필요 — 모의 결제 모드면 false
    private boolean cardRequired;
    private boolean cardRegistered;
    private String cardSummary;

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

    public String getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(String currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public String getPremiumUntil() {
        return premiumUntil;
    }

    public void setPremiumUntil(String premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    public int getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(int monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public boolean isCardRequired() {
        return cardRequired;
    }

    public void setCardRequired(boolean cardRequired) {
        this.cardRequired = cardRequired;
    }

    public boolean isCardRegistered() {
        return cardRegistered;
    }

    public void setCardRegistered(boolean cardRegistered) {
        this.cardRegistered = cardRegistered;
    }

    public String getCardSummary() {
        return cardSummary;
    }

    public void setCardSummary(String cardSummary) {
        this.cardSummary = cardSummary;
    }
}
