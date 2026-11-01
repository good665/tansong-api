package kr.tansong.demo.subscription.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.subscription.mapper.BillingCardMapper;
import kr.tansong.demo.subscription.payment.PaymentGateway;

/**
 * 결제 카드 등록: 앱이 registerUrl 을 브라우저로 열면 → 토스 카드 등록 화면 → 토스가 successUrl 로
 * authKey 를 보내옴 → 빌링키 발급. 브라우저 단계에는 로그인 토큰이 없으므로, 로그인한 사업자가 발급받은
 * 추측 불가능한 customerKey(UUID)가 곧 "이 시설의 등록 요청"이라는 증표 역할을 합니다.
 */
@Service
public class BillingCardService {

    private final BillingCardMapper billingCardMapper;
    private final PaymentGateway paymentGateway;

    public BillingCardService(BillingCardMapper billingCardMapper,
                              PaymentGateway paymentGateway) {
        this.billingCardMapper = billingCardMapper;
        this.paymentGateway = paymentGateway;
    }

    /** @return 브라우저로 열 카드 등록 페이지 경로 — 웹(localhost)/에뮬레이터(10.0.2.2)/실기기마다 서버 주소가 달라서 앱이 자기 API 주소를 앞에 붙임 */
    @Transactional
    public String startRegistration(Long facilityId) {
        if (!paymentGateway.requiresCard()) {
            throw new IllegalArgumentException("모의 결제 모드에서는 카드 등록이 필요 없습니다.");
        }

        // 토스 규칙: 22~50자, 특수문자(-_=.@) 1개 이상, 추측 불가능한 값 → 하이픈 포함 UUID
        String customerKey = UUID.randomUUID().toString();
        billingCardMapper.startRegistration(facilityId, customerKey);
        return "/billing/toss/register?customerKey=" + customerKey;
    }

    /** 토스 successUrl 처리 — 등록 대기 중인 customerKey 가 아니면 거절 */
    @Transactional
    public void completeRegistration(String customerKey, String authKey) {
        if (billingCardMapper.findFacilityIdByPendingKey(customerKey) == null) {
            throw new IllegalArgumentException("만료되었거나 알 수 없는 카드 등록 요청입니다. 앱에서 다시 시도해 주세요.");
        }

        PaymentGateway.IssuedCard card = paymentGateway.issueBillingKey(customerKey, authKey);
        // shortcut: 카드를 바꿀 때 이전 빌링키를 토스에서 삭제(DELETE /v1/billing/{key})하지 않음 — 카드 삭제 기능을 만들 때 함께 처리
        billingCardMapper.completeRegistration(customerKey, card.billingKey(), card.cardSummary());
    }

    public boolean isPendingCustomerKey(String customerKey) {
        return billingCardMapper.findFacilityIdByPendingKey(customerKey) != null;
    }

    public String clientKey() {
        return paymentGateway.clientKey();
    }
}
