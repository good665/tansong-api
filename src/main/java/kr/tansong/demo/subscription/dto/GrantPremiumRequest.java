package kr.tansong.demo.subscription.dto;

public class GrantPremiumRequest {

    private String plan;
    private int periodDays;

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public int getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(int periodDays) {
        this.periodDays = periodDays;
    }
}
