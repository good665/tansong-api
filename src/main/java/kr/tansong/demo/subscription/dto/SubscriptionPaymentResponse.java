package kr.tansong.demo.subscription.dto;

public class SubscriptionPaymentResponse {

    private Long id;
    private int amount;
    private String status;
    private String pgProvider;
    private String paidAt;
    // 이 결제로 노출되는 마지막 날 (yyyy-MM-dd)
    private String coveredUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPgProvider() {
        return pgProvider;
    }

    public void setPgProvider(String pgProvider) {
        this.pgProvider = pgProvider;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    public String getCoveredUntil() {
        return coveredUntil;
    }

    public void setCoveredUntil(String coveredUntil) {
        this.coveredUntil = coveredUntil;
    }
}
