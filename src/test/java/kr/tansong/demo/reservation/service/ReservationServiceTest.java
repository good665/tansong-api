package kr.tansong.demo.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.reservation.dto.ReservationRequest;
import kr.tansong.demo.reservation.mapper.ReservationMapper;

/**
 * 방문 예약의 핵심 비즈니스 규칙(정원 초과, 과거 시간 예약 방지 등)을 검증합니다.
 * ReservationService#createReservation 은 레이스 컨디션 수정(FOR UPDATE)의 근거가 되는
 * "정원을 넘기면 안 된다"는 불변 조건을 직접 보장하는 코드라, 회귀에 가장 취약한 부분입니다.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final int SLOT_CAPACITY = 2;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private FacilityMapper facilityMapper;

    private ReservationService reservationService;

    private String futureDateTime;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationMapper, facilityMapper, SLOT_CAPACITY);
        futureDateTime = LocalDateTime.now().plusDays(7).format(FORMATTER);
    }

    private ReservationRequest validRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setUserId(1L);
        request.setFacilityId(10L);
        request.setScheduledAt(futureDateTime);
        request.setMessage("문의드립니다.");
        return request;
    }

    @Test
    void createReservation_정상_요청이면_insert를_호출하고_1을_반환한다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());
        when(reservationMapper.countActiveByFacilityAndTime(anyLong(), anyString())).thenReturn(0);
        when(reservationMapper.insert(any())).thenReturn(1);

        ReservationRequest request = validRequest();
        int result = reservationService.createReservation(request);

        assertThat(result).isEqualTo(1);
        verify(reservationMapper).insert(request);
    }

    @Test
    void createReservation_정원이_가득_차면_ConflictException을_던지고_insert하지_않는다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());
        when(reservationMapper.countActiveByFacilityAndTime(anyLong(), anyString())).thenReturn(SLOT_CAPACITY);

        assertThatThrownBy(() -> reservationService.createReservation(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("모두 찼습니다");

        verify(reservationMapper, never()).insert(any());
    }

    @Test
    void createReservation_정원보다_하나_적게_차있으면_성공한다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());
        when(reservationMapper.countActiveByFacilityAndTime(anyLong(), anyString()))
                .thenReturn(SLOT_CAPACITY - 1);
        when(reservationMapper.insert(any())).thenReturn(1);

        assertThat(reservationService.createReservation(validRequest())).isEqualTo(1);
    }

    @Test
    void createReservation_존재하지_않는_시설이면_IllegalArgumentException을_던진다() {
        when(facilityMapper.findById(10L)).thenReturn(null);

        assertThatThrownBy(() -> reservationService.createReservation(validRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 시설");

        verify(reservationMapper, never()).countActiveByFacilityAndTime(anyLong(), anyString());
        verify(reservationMapper, never()).insert(any());
    }

    @Test
    void createReservation_과거_시간이면_IllegalArgumentException을_던진다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());

        ReservationRequest request = validRequest();
        request.setScheduledAt(LocalDateTime.now().minusDays(1).format(FORMATTER));

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 시각 이후");

        verify(reservationMapper, never()).insert(any());
    }

    @Test
    void createReservation_날짜_형식이_올바르지_않으면_IllegalArgumentException을_던진다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());

        ReservationRequest request = validRequest();
        request.setScheduledAt("2026-13-99 25:99");

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("형식이 올바르지 않습니다");
    }

    @Test
    void createReservation_요청사항이_1000자를_초과하면_IllegalArgumentException을_던진다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());

        ReservationRequest request = validRequest();
        request.setMessage("a".repeat(1001));

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000자");

        verify(reservationMapper, never()).insert(any());
    }

    @Test
    void createReservation_userId가_없으면_IllegalArgumentException을_던진다() {
        ReservationRequest request = validRequest();
        request.setUserId(null);

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 ID");
    }

    @Test
    void createReservation_facilityId가_0_이하이면_IllegalArgumentException을_던진다() {
        ReservationRequest request = validRequest();
        request.setFacilityId(0L);

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시설 ID");
    }
}
