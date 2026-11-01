package kr.tansong.demo.consultation.service;

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
import kr.tansong.demo.consultation.dto.ConsultationRequest;
import kr.tansong.demo.consultation.mapper.ConsultationMapper;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.FacilityMapper;

/** 상담 신청 시간대 정원 규칙 */
@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    private static final int SLOT_CAPACITY = 2;

    @Mock
    private ConsultationMapper consultationMapper;

    @Mock
    private FacilityMapper facilityMapper;

    private ConsultationService consultationService;

    @BeforeEach
    void setUp() {
        consultationService = new ConsultationService(consultationMapper, facilityMapper, SLOT_CAPACITY);
    }

    private ConsultationRequest request(String preferredAt) {
        ConsultationRequest request = new ConsultationRequest();
        request.setUserId(1L);
        request.setFacilityId(10L);
        request.setConsultationMethod("PHONE");
        request.setPreferredAt(preferredAt);
        return request;
    }

    private String future() {
        return LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    @Test
    void 정원_미만이면_신청된다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());
        when(consultationMapper.countActiveByFacilityAndTime(anyLong(), anyString())).thenReturn(SLOT_CAPACITY - 1);
        when(consultationMapper.insert(any())).thenReturn(1);

        assertThat(consultationService.createConsultation(request(future()))).isEqualTo(1);
    }

    @Test
    void 정원이_차면_409이고_저장하지_않는다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());
        when(consultationMapper.countActiveByFacilityAndTime(anyLong(), anyString())).thenReturn(SLOT_CAPACITY);

        assertThatThrownBy(() -> consultationService.createConsultation(request(future())))
                .isInstanceOf(ConflictException.class);
        verify(consultationMapper, never()).insert(any());
    }

    @Test
    void 희망시간이_없으면_정원을_확인하지_않는다() {
        when(facilityMapper.findById(10L)).thenReturn(new FacilityResponse());
        when(consultationMapper.insert(any())).thenReturn(1);

        assertThat(consultationService.createConsultation(request(null))).isEqualTo(1);
        verify(consultationMapper, never()).countActiveByFacilityAndTime(anyLong(), anyString());
    }
}
