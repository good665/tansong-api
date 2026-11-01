package kr.tansong.demo.admin.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.subscription.dto.GrantPremiumRequest;
import kr.tansong.demo.subscription.dto.SubscriptionResponse;
import kr.tansong.demo.subscription.service.SubscriptionRenewalScheduler;
import kr.tansong.demo.subscription.service.SubscriptionService;

/** 시설 상단노출 구독 관리 (수동 부여/강제 해지/자동 연장 즉시 실행).
 *  SecurityConfig 에서 /api/admin/** 를 ROLE_ADMIN 으로 제한 */
@RestController
@RequestMapping("/api/admin")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRenewalScheduler renewalScheduler;

    public AdminSubscriptionController(SubscriptionService subscriptionService,
                                       SubscriptionRenewalScheduler renewalScheduler) {
        this.subscriptionService = subscriptionService;
        this.renewalScheduler = renewalScheduler;
    }

    @GetMapping("/facilities/{facilityId}/subscription")
    public SubscriptionResponse getStatus(@PathVariable Long facilityId) {
        return subscriptionService.getStatus(facilityId);
    }

    @PutMapping("/facilities/{facilityId}/subscription")
    public Map<String, Object> grant(@PathVariable Long facilityId, @RequestBody GrantPremiumRequest request) {
        subscriptionService.grantPremium(facilityId, request == null ? null : request.getPlan(),
                request == null ? 0 : request.getPeriodDays());
        return Map.of("success", true, "message", "상단 노출이 적용되었습니다.");
    }

    @DeleteMapping("/facilities/{facilityId}/subscription")
    public Map<String, Object> revoke(@PathVariable Long facilityId) {
        subscriptionService.revoke(facilityId);
        return Map.of("success", true, "message", "상단 노출이 해지되었습니다.");
    }

    @GetMapping("/subscriptions/overview")
    public Map<String, Object> overview() {
        return subscriptionService.getOverview();
    }

    // 매일 23:50 에 도는 자동 연장을 지금 바로 실행 (운영 점검/테스트용)
    @PostMapping("/subscriptions/renewals")
    public Map<String, Object> runRenewals() {
        int renewed = renewalScheduler.runOnce();
        return Map.of("success", true, "renewed", renewed);
    }
}
