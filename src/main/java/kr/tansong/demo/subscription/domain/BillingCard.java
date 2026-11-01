package kr.tansong.demo.subscription.domain;

/** billing_cards 테이블 한 행 (시설당 하나). billingKey 가 null 이면 아직 등록이 끝나지 않은 상태 */
public class BillingCard {

    private Long facilityId;
    private String customerKey;
    private String billingKey;
    private String cardSummary;

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public String getBillingKey() {
        return billingKey;
    }

    public void setBillingKey(String billingKey) {
        this.billingKey = billingKey;
    }

    public String getCardSummary() {
        return cardSummary;
    }

    public void setCardSummary(String cardSummary) {
        this.cardSummary = cardSummary;
    }
}
