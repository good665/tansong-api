package kr.tansong.demo.facility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.facility.mapper.FacilitySearchMapper;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse;
import kr.tansong.demo.facilitydetail.service.FacilityDetailsService;

/** 광고가 유효한 시설의 상세 조회에만 추가 정보가 포함됨 */
@ExtendWith(MockitoExtension.class)
class FacilityServiceDetailsTest {

    @Mock
    private FacilityMapper facilityMapper;
    @Mock
    private FacilitySearchMapper facilitySearchMapper;
    @Mock
    private FacilityImageMapper facilityImageMapper;
    @Mock
    private FacilityDetailsService facilityDetailsService;

    private FacilityService service;

    @BeforeEach
    void setUp() {
        service = new FacilityService(facilityMapper, facilitySearchMapper, facilityImageMapper, facilityDetailsService);
    }

    private FacilityResponse facility(boolean premium) {
        FacilityResponse facility = new FacilityResponse();
        facility.setId(10L);
        facility.setPremium(premium);
        when(facilityMapper.findById(10L)).thenReturn(facility);
        return facility;
    }

    @Test
    void 광고_유효하면_premiumDetails_채움() {
        facility(true);
        FacilityDetailsResponse details = new FacilityDetailsResponse();
        when(facilityDetailsService.getPublic(10L)).thenReturn(details);

        assertThat(service.getFacilityById(10L).getPremiumDetails()).isSameAs(details);
    }

    @Test
    void 광고_만료면_null이고_조회하지_않음() {
        facility(false);

        assertThat(service.getFacilityById(10L).getPremiumDetails()).isNull();
        verify(facilityDetailsService, never()).getPublic(any());
    }

    @Test
    void 시설이_없으면_null() {
        when(facilityMapper.findById(10L)).thenReturn(null);

        assertThat(service.getFacilityById(10L)).isNull();
        verify(facilityDetailsService, never()).getPublic(any());
    }
}
