package kr.tansong.demo.business.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.business.service.BusinessFacilityService;
import kr.tansong.demo.subscription.dto.SubscriptionPaymentResponse;
import kr.tansong.demo.subscription.dto.SubscriptionResponse;
import kr.tansong.demo.subscription.service.BillingCardService;
import kr.tansong.demo.subscription.service.SubscriptionService;

/** 사업자가 본인 소유 시설의 상단노출(광고) 구독을 결제/해지/조회.
 *  SecurityConfig 에서 /api/business/** 를 ROLE_BUSINESS_OWNER 로 제한하고,
 *  모든 요청은 먼저 소유권을 확인합니다 (남의 시설이면 NotFoundException → 404). */
@RestController
@RequestMapping("/api/business/facilities/{facilityId}/subscription")
public class BusinessSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final BusinessFacilityService businessFacilityService;
    private final BillingCardService billingCardService;

    public BusinessSubscriptionController(SubscriptionService subscriptionService,
                                          BusinessFacilityService businessFacilityService,
                                          BillingCardService billingCardService) {
        this.subscriptionService = subscriptionService;
        this.businessFacilityService = businessFacilityService;
        this.billingCardService = billingCardService;
    }

    // 결제 카드 등록 시작 — 돌려준 주소를 앱이 브라우저로 엽니다
    @PostMapping("/card-registration")
    public Map<String, String> startCardRegistration(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        businessFacilityService.getById(facilityId, ownerUserId);
        return Map.of("registerUrl", billingCardService.startRegistration(facilityId));
    }

    @GetMapping
    public SubscriptionResponse getStatus(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        businessFacilityService.getById(facilityId, ownerUserId);
        return subscriptionService.getStatus(facilityId);
    }

    // 상단노출 결제 (자동 연장이 꺼진 채 노출 중이면 결제 없이 자동 연장만 다시 켬)
    @PostMapping
    public SubscriptionResponse subscribe(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        businessFacilityService.getById(facilityId, ownerUserId);
        return subscriptionService.subscribe(facilityId);
    }

    // 자동 연장 해지 (결제한 기간 끝까지는 계속 노출)
    @DeleteMapping
    public SubscriptionResponse cancelAutoRenew(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        businessFacilityService.getById(facilityId, ownerUserId);
        return subscriptionService.cancelAutoRenew(facilityId);
    }

    @GetMapping("/payments")
    public List<SubscriptionPaymentResponse> getPayments(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        businessFacilityService.getById(facilityId, ownerUserId);
        return subscriptionService.getPayments(facilityId);
    }
}
