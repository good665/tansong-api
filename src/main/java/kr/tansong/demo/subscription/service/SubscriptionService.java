package kr.tansong.demo.subscription.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.subscription.domain.BillingCard;
import kr.tansong.demo.subscription.domain.Subscription;
import kr.tansong.demo.subscription.dto.SubscriptionPaymentResponse;
import kr.tansong.demo.subscription.dto.SubscriptionResponse;
import kr.tansong.demo.subscription.mapper.BillingCardMapper;
import kr.tansong.demo.subscription.mapper.SubscriptionMapper;
import kr.tansong.demo.subscription.payment.PaymentGateway;
import kr.tansong.demo.subscription.payment.PaymentResult;

/**
 * 시설 상단노출(광고) 월 구독. 기간은 날짜 단위로 끊습니다 —
 * 10/30 에 결제하면 11/29 까지 노출되고 11/30 00:00 에 내려갑니다 (current_period_end = 11/30 00:00).
 * 자동 연장이 켜진 구독은 SubscriptionRenewalScheduler 가 기간 마지막 날 밤에 다음 달을 청구합니다.
 */
@Service
public class SubscriptionService {

    public static final String PLAN = "PREMIUM";
    public static final String STATUS_ACTIVE = "ACTIVE";       // 자동 연장 켜짐
    public static final String STATUS_CANCELED = "CANCELED";   // 자동 연장 꺼짐 (기간 끝까지는 노출)
    public static final String STATUS_EXPIRED = "EXPIRED";     // 연장 실패 등으로 종료

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SubscriptionMapper subscriptionMapper;
    private final BillingCardMapper billingCardMapper;
    private final FacilityMapper facilityMapper;
    private final PaymentGateway paymentGateway;
    private final Clock clock;
    private final int monthlyPrice;

    public SubscriptionService(SubscriptionMapper subscriptionMapper,
                               BillingCardMapper billingCardMapper,
                               FacilityMapper facilityMapper,
                               PaymentGateway paymentGateway,
                               Clock clock,
                               @Value("${app.subscription.monthly-price:1000}") int monthlyPrice) {
        this.subscriptionMapper = subscriptionMapper;
        this.billingCardMapper = billingCardMapper;
        this.facilityMapper = facilityMapper;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
        this.monthlyPrice = monthlyPrice;
    }

    public SubscriptionResponse getStatus(Long facilityId) {
        return toResponse(facilityId, subscriptionMapper.findByFacilityId(facilityId));
    }

    public List<SubscriptionPaymentResponse> getPayments(Long facilityId) {
        return subscriptionMapper.findPaymentsByFacilityId(facilityId);
    }

    /**
     * 사업자가 상단노출을 신청합니다.
     * - 노출 중이 아니면: 이번 달 요금을 결제하고 오늘부터 한 달 노출, 자동 연장 켜짐
     * - 노출 중인데 자동 연장만 꺼져 있으면: 추가 결제 없이 자동 연장만 다시 켬
     * - 이미 노출 중이고 자동 연장도 켜져 있으면: 409
     */
    @Transactional
    public SubscriptionResponse subscribe(Long facilityId) {
        requireApprovedFacility(facilityId);

        Subscription current = subscriptionMapper.findByFacilityIdForUpdate(facilityId);
        LocalDateTime now = LocalDateTime.now(clock);

        if (isActive(current, now) && STATUS_ACTIVE.equals(current.getStatus())) {
            throw new ConflictException("이미 상단 노출 중입니다.");
        }

        // 자동 연장을 다시 켜는 경우도 다음 달 청구에 카드가 필요하므로 여기서 함께 확인
        BillingCard card = billingCardMapper.findRegistered(facilityId);
        if (paymentGateway.requiresCard() && card == null) {
            throw new IllegalArgumentException("결제 카드를 먼저 등록해 주세요.");
        }

        if (isActive(current, now)) {
            subscriptionMapper.updateStatus(facilityId, STATUS_ACTIVE);
            return getStatus(facilityId);
        }

        LocalDateTime periodEnd = LocalDate.now(clock).plusMonths(1).atStartOfDay();
        PaymentResult result = paymentGateway.charge(card, facilityId, monthlyPrice, "시설 상단노출 1개월");

        if (!result.success()) {
            throw new IllegalArgumentException("결제에 실패했습니다. " + nullToEmpty(result.failureReason()));
        }

        subscriptionMapper.upsert(facilityId, PLAN, STATUS_ACTIVE, periodEnd);
        recordPayment(facilityId, result, periodEnd);
        return getStatus(facilityId);
    }

    /** 자동 연장 해지 — 이미 결제한 기간이 끝날 때까지는 계속 노출되고, 그 다음날 내려갑니다. */
    @Transactional
    public SubscriptionResponse cancelAutoRenew(Long facilityId) {
        Subscription current = subscriptionMapper.findByFacilityIdForUpdate(facilityId);

        if (!isActive(current, LocalDateTime.now(clock)) || !STATUS_ACTIVE.equals(current.getStatus())) {
            throw new ConflictException("자동 연장 중인 구독이 없습니다.");
        }

        subscriptionMapper.updateStatus(facilityId, STATUS_CANCELED);
        return getStatus(facilityId);
    }

    /** 오늘 자정까지 기간이 끝나는(또는 이미 끝난) 자동 연장 구독의 시설 ID 목록 */
    public List<Long> findDueForRenewal() {
        LocalDateTime cutoff = LocalDate.now(clock).plusDays(1).atStartOfDay();
        return subscriptionMapper.findFacilityIdsDueForRenewal(cutoff);
    }

    /**
     * 한 시설의 자동 연장 결제. 스케줄러가 시설마다 따로 호출하므로 시설별 별도 트랜잭션이고,
     * 행을 다시 잠그고 조건을 재확인하므로 같은 날 두 번 실행돼도 이중 청구되지 않습니다.
     * @return 연장에 성공했으면 true
     */
    @Transactional
    public boolean renew(Long facilityId) {
        Subscription current = subscriptionMapper.findByFacilityIdForUpdate(facilityId);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime cutoff = today.plusDays(1).atStartOfDay();

        if (current == null || !STATUS_ACTIVE.equals(current.getStatus())
                || current.getCurrentPeriodEnd().isAfter(cutoff)) {
            return false; // 이미 연장됐거나 해지됨
        }

        // 승인이 취소(비활성화)된 시설은 더 이상 청구하지 않음
        if (!"ACTIVE".equals(facilityMapper.findStatusById(facilityId))) {
            subscriptionMapper.updateStatus(facilityId, STATUS_EXPIRED);
            return false;
        }

        // 정상이면 기존 만료일부터 한 달, 서버가 멈춰 있어 이미 만료됐다면 오늘부터 한 달
        LocalDate base = current.getCurrentPeriodEnd().toLocalDate();
        if (base.isBefore(today)) {
            base = today;
        }
        LocalDateTime periodEnd = base.plusMonths(1).atStartOfDay();

        PaymentResult result = paymentGateway.charge(
                billingCardMapper.findRegistered(facilityId), facilityId, monthlyPrice, "시설 상단노출 1개월 자동 연장");
        recordPayment(facilityId, result, result.success() ? periodEnd : null);

        if (!result.success()) {
            // 결제한 기간이 끝나면 자연스럽게 상단에서 내려감
            subscriptionMapper.updateStatus(facilityId, STATUS_EXPIRED);
            return false;
        }

        subscriptionMapper.upsert(facilityId, PLAN, STATUS_ACTIVE, periodEnd);
        return true;
    }

    /** 관리자 수동 부여 (결제 없음, 자동 연장 없음) — 오늘부터 periodDays 일 동안 노출 */
    @Transactional
    public void grantPremium(Long facilityId, String plan, int periodDays) {
        if (facilityId == null || facilityId <= 0 || facilityMapper.countById(facilityId) == 0) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
        if (plan == null || !PLAN.equals(plan)) {
            throw new IllegalArgumentException("지원하지 않는 요금제입니다.");
        }
        if (periodDays <= 0 || periodDays > 365) {
            throw new IllegalArgumentException("구독 기간은 1~365일 사이로 입력해 주세요.");
        }

        LocalDateTime periodEnd = LocalDate.now(clock).plusDays(periodDays).atStartOfDay();
        subscriptionMapper.upsert(facilityId, plan, STATUS_CANCELED, periodEnd);
    }

    /** 관리자 강제 해지 — 즉시 상단에서 내려감 */
    @Transactional
    public void revoke(Long facilityId) {
        if (facilityId == null || facilityId <= 0 || facilityMapper.countById(facilityId) == 0) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }

        subscriptionMapper.upsert(facilityId, PLAN, STATUS_CANCELED, LocalDateTime.now(clock));
    }

    private void requireApprovedFacility(Long facilityId) {
        String status = facilityMapper.findStatusById(facilityId);

        if (status == null) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
        if (!"ACTIVE".equals(status)) {
            throw new IllegalArgumentException("관리자 승인 후 상단 노출을 신청할 수 있습니다.");
        }
    }

    private void recordPayment(Long facilityId, PaymentResult result, LocalDateTime coveredUntil) {
        subscriptionMapper.insertPayment(
                facilityId,
                monthlyPrice,
                result.success() ? "PAID" : "FAILED",
                paymentGateway.providerName(),
                result.transactionId(),
                result.failureReason(),
                coveredUntil);
    }

    private boolean isActive(Subscription subscription, LocalDateTime now) {
        return subscription != null && subscription.getCurrentPeriodEnd().isAfter(now);
    }

    private SubscriptionResponse toResponse(Long facilityId, Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setFacilityId(facilityId);
        response.setMonthlyPrice(monthlyPrice);
        response.setCardRequired(paymentGateway.requiresCard());
        if (paymentGateway.requiresCard()) {
            BillingCard card = billingCardMapper.findRegistered(facilityId);
            response.setCardRegistered(card != null);
            response.setCardSummary(card == null ? null : card.getCardSummary());
        }

        if (subscription == null) {
            // 한 번도 구독한 적 없는 시설
            response.setPlan("FREE");
            response.setStatus("NONE");
            return response;
        }

        boolean active = isActive(subscription, LocalDateTime.now(clock));
        response.setPlan(subscription.getPlan());
        response.setStatus(subscription.getStatus());
        response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().format(DATE_TIME));
        response.setActive(active);
        response.setAutoRenew(active && STATUS_ACTIVE.equals(subscription.getStatus()));
        if (active) {
            response.setPremiumUntil(subscription.getCurrentPeriodEnd().minusDays(1).format(DATE));
        }
        return response;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
