package kr.tansong.demo.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.subscription.domain.Subscription;
import kr.tansong.demo.subscription.dto.SubscriptionResponse;
import kr.tansong.demo.subscription.mapper.SubscriptionMapper;
import kr.tansong.demo.subscription.payment.PaymentGateway;
import kr.tansong.demo.subscription.payment.PaymentResult;

/**
 * 상단노출 구독의 기간 계산(만료 다음날 내려감), 이중 결제 방지, 자동 연장/해지 규칙을 검증합니다.
 * 기준 시각은 2026-10-30 10:00 KST 로 고정.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final Long FACILITY_ID = 7L;
    private static final int PRICE = 1000;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 10, 0);

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private FacilityMapper facilityMapper;

    @Mock
    private PaymentGateway paymentGateway;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);
        service = new SubscriptionService(subscriptionMapper, facilityMapper, paymentGateway, clock, PRICE);
        lenient().when(paymentGateway.providerName()).thenReturn("MOCK");
    }

    private Subscription subscription(String status, LocalDateTime periodEnd) {
        Subscription s = new Subscription();
        s.setFacilityId(FACILITY_ID);
        s.setPlan("PREMIUM");
        s.setStatus(status);
        s.setCurrentPeriodEnd(periodEnd);
        return s;
    }

    @Test
    void subscribe_처음이면_결제하고_한달뒤_다음날_0시에_만료된다() {
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("ACTIVE");
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID)).thenReturn(null);
        when(paymentGateway.charge(eq(FACILITY_ID), eq(PRICE), anyString())).thenReturn(PaymentResult.success("tx-1"));

        service.subscribe(FACILITY_ID);

        // 10/30 결제 → 11/29 까지 노출, 11/30 00:00 만료
        LocalDateTime expectedEnd = LocalDateTime.of(2026, 11, 30, 0, 0);
        verify(subscriptionMapper).upsert(FACILITY_ID, "PREMIUM", "ACTIVE", expectedEnd);
        verify(subscriptionMapper).insertPayment(FACILITY_ID, PRICE, "PAID", "MOCK", "tx-1", null, expectedEnd);
    }

    @Test
    void subscribe_승인전_시설이면_결제하지_않는다() {
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("PENDING");

        assertThatThrownBy(() -> service.subscribe(FACILITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("승인");
        verify(paymentGateway, never()).charge(anyLong(), anyInt(), anyString());
    }

    @Test
    void subscribe_이미_자동연장_중이면_409이고_이중결제하지_않는다() {
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("ACTIVE");
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.of(2026, 11, 15, 0, 0)));

        assertThatThrownBy(() -> service.subscribe(FACILITY_ID)).isInstanceOf(ConflictException.class);
        verify(paymentGateway, never()).charge(anyLong(), anyInt(), anyString());
    }

    @Test
    void subscribe_해지예약_상태면_결제없이_자동연장만_다시_켠다() {
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("ACTIVE");
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("CANCELED", LocalDateTime.of(2026, 11, 15, 0, 0)));

        service.subscribe(FACILITY_ID);

        verify(subscriptionMapper).updateStatus(FACILITY_ID, "ACTIVE");
        verify(paymentGateway, never()).charge(anyLong(), anyInt(), anyString());
    }

    @Test
    void subscribe_결제실패면_구독을_만들지_않는다() {
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("ACTIVE");
        when(paymentGateway.charge(eq(FACILITY_ID), eq(PRICE), anyString()))
                .thenReturn(PaymentResult.failure("카드 한도 초과"));

        assertThatThrownBy(() -> service.subscribe(FACILITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("카드 한도 초과");
        verify(subscriptionMapper, never()).upsert(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void cancel_자동연장을_끄고_기간끝까지는_노출된다() {
        LocalDateTime end = LocalDateTime.of(2026, 11, 30, 0, 0);
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID)).thenReturn(subscription("ACTIVE", end));
        when(subscriptionMapper.findByFacilityId(FACILITY_ID)).thenReturn(subscription("CANCELED", end));

        SubscriptionResponse response = service.cancelAutoRenew(FACILITY_ID);

        verify(subscriptionMapper).updateStatus(FACILITY_ID, "CANCELED");
        assertThat(response.isActive()).isTrue();
        assertThat(response.isAutoRenew()).isFalse();
        assertThat(response.getPremiumUntil()).isEqualTo("2026-11-29");
    }

    @Test
    void getStatus_기간이_지났으면_노출되지_않는다() {
        when(subscriptionMapper.findByFacilityId(FACILITY_ID))
                .thenReturn(subscription("CANCELED", LocalDateTime.of(2026, 10, 30, 0, 0)));

        SubscriptionResponse response = service.getStatus(FACILITY_ID);

        assertThat(response.isActive()).isFalse();
        assertThat(response.getPremiumUntil()).isNull();
    }

    @Test
    void renew_오늘_자정에_끝나는_구독을_한달_연장한다() {
        // 10/31 00:00 만료 = 오늘(10/30)이 마지막 노출일
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.of(2026, 10, 31, 0, 0)));
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("ACTIVE");
        when(paymentGateway.charge(eq(FACILITY_ID), eq(PRICE), anyString())).thenReturn(PaymentResult.success("tx-2"));

        assertThat(service.renew(FACILITY_ID)).isTrue();

        LocalDateTime expectedEnd = LocalDateTime.of(2026, 11, 30, 0, 0);
        verify(subscriptionMapper).upsert(FACILITY_ID, "PREMIUM", "ACTIVE", expectedEnd);
    }

    @Test
    void renew_이미_연장된_구독은_다시_청구하지_않는다() {
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.of(2026, 11, 30, 0, 0)));

        assertThat(service.renew(FACILITY_ID)).isFalse();
        verify(paymentGateway, never()).charge(anyLong(), anyInt(), anyString());
    }

    @Test
    void renew_해지예약된_구독은_청구하지_않는다() {
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("CANCELED", LocalDateTime.of(2026, 10, 31, 0, 0)));

        assertThat(service.renew(FACILITY_ID)).isFalse();
        verify(paymentGateway, never()).charge(anyLong(), anyInt(), anyString());
    }

    @Test
    void renew_결제실패면_EXPIRED로_바꾸고_실패이력을_남긴다() {
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.of(2026, 10, 31, 0, 0)));
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("ACTIVE");
        when(paymentGateway.charge(eq(FACILITY_ID), eq(PRICE), anyString()))
                .thenReturn(PaymentResult.failure("잔액 부족"));

        assertThat(service.renew(FACILITY_ID)).isFalse();

        verify(subscriptionMapper).updateStatus(FACILITY_ID, "EXPIRED");
        verify(subscriptionMapper).insertPayment(eq(FACILITY_ID), eq(PRICE), eq("FAILED"), eq("MOCK"),
                isNull(), eq("잔액 부족"), isNull());
        verify(subscriptionMapper, never()).upsert(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void renew_비활성화된_시설은_청구하지_않고_종료한다() {
        when(subscriptionMapper.findByFacilityIdForUpdate(FACILITY_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.of(2026, 10, 31, 0, 0)));
        when(facilityMapper.findStatusById(FACILITY_ID)).thenReturn("INACTIVE");

        assertThat(service.renew(FACILITY_ID)).isFalse();

        verify(subscriptionMapper).updateStatus(FACILITY_ID, "EXPIRED");
        verify(paymentGateway, never()).charge(anyLong(), anyInt(), anyString());
    }
}
