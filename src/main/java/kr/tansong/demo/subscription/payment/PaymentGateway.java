package kr.tansong.demo.subscription.payment;

/** 정기결제 청구 창구. 지금은 MockPaymentGateway 하나뿐이고, 실제 PG(토스페이먼츠/포트원 등)를
 *  연동할 때 이 인터페이스의 구현체만 추가하면 SubscriptionService 는 그대로 재사용됩니다. */
public interface PaymentGateway {

    /** PG 이름 — 결제 이력(subscription_payments.pg_provider)에 남습니다. */
    String providerName();

    /** facilityId 시설의 구독료 amount 원을 청구합니다. 실패해도 예외 대신 실패 결과를 돌려줍니다. */
    PaymentResult charge(Long facilityId, int amount, String orderName);
}
