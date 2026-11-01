package kr.tansong.demo.subscription.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** PG 연동 전 임시 결제 — 실제 돈은 오가지 않고 항상 성공으로 처리합니다.
 *  실제 PG 구현체를 추가할 때 이 클래스의 @Component 를 제거(또는 프로필로 분리)하세요. */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public PaymentResult charge(Long facilityId, int amount, String orderName) {
        return PaymentResult.success("MOCK-" + UUID.randomUUID());
    }
}
