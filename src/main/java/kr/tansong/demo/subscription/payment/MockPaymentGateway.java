package kr.tansong.demo.subscription.payment;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import kr.tansong.demo.subscription.domain.BillingCard;

/** PG 키가 없을 때 쓰는 임시 결제 — 실제 돈은 오가지 않고 항상 성공으로 처리합니다. */
@Component
@ConditionalOnExpression("'${app.toss.secret-key:}'.isBlank()")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public boolean requiresCard() {
        return false;
    }

    @Override
    public String clientKey() {
        return null;
    }

    @Override
    public IssuedCard issueBillingKey(String customerKey, String authKey) {
        throw new IllegalArgumentException("모의 결제 모드에서는 카드 등록이 필요 없습니다.");
    }

    @Override
    public PaymentResult charge(BillingCard card, Long facilityId, int amount, String orderName) {
        return PaymentResult.success("MOCK-" + UUID.randomUUID());
    }
}
