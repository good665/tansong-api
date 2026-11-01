package kr.tansong.demo.subscription.payment;

import kr.tansong.demo.subscription.domain.BillingCard;

/** 정기결제 창구. TOSS_SECRET_KEY 가 있으면 TossPaymentGateway, 없으면 MockPaymentGateway 가 쓰입니다. */
public interface PaymentGateway {

    /** PG 이름 — 결제 이력(subscription_payments.pg_provider)에 남습니다. */
    String providerName();

    /** 청구 전에 카드(빌링키) 등록이 필요한지 — 모의 결제는 false */
    boolean requiresCard();

    /** 카드 등록 화면(SDK)에 넣을 클라이언트 키 — 카드 등록이 없는 PG 면 null */
    String clientKey();

    /** 카드 등록 화면이 돌려준 authKey 로 빌링키를 발급받습니다. */
    IssuedCard issueBillingKey(String customerKey, String authKey);

    /** 구독료 청구. card 는 requiresCard()=false 면 null 일 수 있습니다. 실패해도 예외 대신 실패 결과를 돌려줍니다. */
    PaymentResult charge(BillingCard card, Long facilityId, int amount, String orderName);

    record IssuedCard(String billingKey, String cardSummary) {
    }
}
