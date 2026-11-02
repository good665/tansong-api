package kr.tansong.demo.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.mapper.BusinessRequestMapper;

/** 사업자는 자기 시설의 신청만, 정해진 순서로만 상태를 바꿀 수 있음 */
@ExtendWith(MockitoExtension.class)
class BusinessRequestServiceTest {

    private static final Long OWNER = 1L;

    @Mock
    private BusinessRequestMapper mapper;

    private BusinessRequestService service;

    @BeforeEach
    void setUp() {
        service = new BusinessRequestService(mapper);
    }

    @ParameterizedTest
    @CsvSource({"REQUESTED,CONFIRMED", "REQUESTED,CANCELED", "CONFIRMED,COMPLETED", "CONFIRMED,CANCELED"})
    void 허용된_순서로_상담_상태를_바꾼다(String from, String to) {
        when(mapper.findConsultationStatus(5L, OWNER)).thenReturn(from);
        when(mapper.updateConsultationStatus(5L, OWNER, from, to)).thenReturn(1);

        assertThatCode(() -> service.updateConsultationStatus(5L, OWNER, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"REQUESTED,COMPLETED", "REQUESTED,REQUESTED", "CONFIRMED,REQUESTED", "COMPLETED,CANCELED", "CANCELED,CONFIRMED", "REQUESTED,NOPE"})
    void 허용되지_않은_상태_변경은_거절한다(String from, String to) {
        when(mapper.findConsultationStatus(5L, OWNER)).thenReturn(from);

        assertThatThrownBy(() -> service.updateConsultationStatus(5L, OWNER, to))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).updateConsultationStatus(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void 상태값이_없으면_거절한다() {
        when(mapper.findConsultationStatus(5L, OWNER)).thenReturn("REQUESTED");

        assertThatThrownBy(() -> service.updateConsultationStatus(5L, OWNER, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 다른_사업자_시설의_상담은_404() {
        when(mapper.findConsultationStatus(5L, OWNER)).thenReturn(null);

        assertThatThrownBy(() -> service.updateConsultationStatus(5L, OWNER, "CONFIRMED"))
                .isInstanceOf(NotFoundException.class);
        verify(mapper, never()).updateConsultationStatus(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void 처리_중_고객이_먼저_취소하면_409() {
        when(mapper.findConsultationStatus(5L, OWNER)).thenReturn("REQUESTED");
        when(mapper.updateConsultationStatus(5L, OWNER, "REQUESTED", "CONFIRMED")).thenReturn(0);

        assertThatThrownBy(() -> service.updateConsultationStatus(5L, OWNER, "CONFIRMED"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 방문_예약도_같은_규칙을_따른다() {
        when(mapper.findReservationStatus(5L, OWNER)).thenReturn("REQUESTED", "COMPLETED", null, "REQUESTED");
        when(mapper.updateReservationStatus(5L, OWNER, "REQUESTED", "CONFIRMED")).thenReturn(1, 0);

        assertThatCode(() -> service.updateReservationStatus(5L, OWNER, "CONFIRMED")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.updateReservationStatus(5L, OWNER, "CANCELED"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateReservationStatus(5L, OWNER, "CONFIRMED"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.updateReservationStatus(5L, OWNER, "CONFIRMED"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 목록은_사업자_범위로_조회하고_ALL은_전체다() {
        when(mapper.countConsultations(OWNER, null)).thenReturn(0L);
        when(mapper.findConsultations(OWNER, null, 10, 20)).thenReturn(List.of());

        var page = service.listConsultations(OWNER, "ALL", 2, 10);

        assertThat(page.getItems()).isEmpty();
        verify(mapper).findConsultations(OWNER, null, 10, 20);
        verify(mapper, never()).findReservations(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void 방문_예약_목록은_상태로_거른다() {
        when(mapper.countReservations(OWNER, "CONFIRMED")).thenReturn(0L);
        when(mapper.findReservations(OWNER, "CONFIRMED", 10, 0)).thenReturn(List.of());

        service.listReservations(OWNER, " CONFIRMED ", 0, 10);

        verify(mapper).findReservations(OWNER, "CONFIRMED", 10, 0);
    }
}
