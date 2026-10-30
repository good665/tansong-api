package kr.tansong.demo.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.tansong.demo.admin.dto.FacilityUpsertRequest;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.BusinessFacilityMapper;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.OrganizationMapper;

/**
 * 사업자가 본인 소유가 아닌 기관/시설을 등록·수정할 수 없는지(소유권 검증)를 집중적으로 검증합니다 —
 * 이게 뚫리면 다른 사업자의 시설을 조회/수정할 수 있게 되는 심각한 취약점이 됩니다.
 */
@ExtendWith(MockitoExtension.class)
class BusinessFacilityServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_OWNER_FACILITY_ID = 99L;

    @Mock
    private BusinessFacilityMapper businessFacilityMapper;

    @Mock
    private OrganizationMapper organizationMapper;

    @Mock
    private FacilityImageMapper facilityImageMapper;

    private BusinessFacilityService service;

    private FacilityUpsertRequest validRequest() {
        FacilityUpsertRequest request = new FacilityUpsertRequest();
        request.setOrganizationId(1L);
        request.setFacilityType("DAY_CARE");
        request.setName("테스트 주야간보호센터");
        request.setAddress("서울시 어딘가 123");
        request.setPhone("02-1234-5678");
        return request;
    }

    @Test
    void create_본인소유기관이면_성공한다() {
        service = new BusinessFacilityService(businessFacilityMapper, organizationMapper, facilityImageMapper);
        when(organizationMapper.countOwnedByUser(1L, OWNER_ID)).thenReturn(1);

        FacilityUpsertRequest request = validRequest();
        service.create(OWNER_ID, request);

        verify(businessFacilityMapper).insert(request);
    }

    @Test
    void create_본인소유가_아닌기관이면_IllegalArgumentException을_던지고_insert하지_않는다() {
        service = new BusinessFacilityService(businessFacilityMapper, organizationMapper, facilityImageMapper);
        when(organizationMapper.countOwnedByUser(1L, OWNER_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.create(OWNER_ID, validRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인 소유");

        verify(businessFacilityMapper, never()).insert(any());
    }

    @Test
    void getById_본인소유가_아닌시설이면_NotFoundException을_던진다() {
        service = new BusinessFacilityService(businessFacilityMapper, organizationMapper, facilityImageMapper);
        when(businessFacilityMapper.findByIdForOwner(OTHER_OWNER_FACILITY_ID, OWNER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getById(OTHER_OWNER_FACILITY_ID, OWNER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_본인소유시설이면_사진을_포함해_반환한다() {
        service = new BusinessFacilityService(businessFacilityMapper, organizationMapper, facilityImageMapper);

        FacilityResponse facility = new FacilityResponse();
        facility.setId(5L);
        when(businessFacilityMapper.findByIdForOwner(5L, OWNER_ID)).thenReturn(facility);
        when(facilityImageMapper.findImageUrlsByFacilityId(5L)).thenReturn(java.util.List.of("url1"));

        FacilityResponse result = service.getById(5L, OWNER_ID);

        assertThat(result.getPhotos()).containsExactly("url1");
    }

    @Test
    void update_본인소유가_아닌시설이면_requireOwnedFacility에서_바로_NotFoundException() {
        service = new BusinessFacilityService(businessFacilityMapper, organizationMapper, facilityImageMapper);
        when(businessFacilityMapper.findByIdForOwner(OTHER_OWNER_FACILITY_ID, OWNER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.update(OTHER_OWNER_FACILITY_ID, OWNER_ID, validRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(businessFacilityMapper, never()).updateForOwner(anyLong(), anyLong(), any());
    }

    @Test
    void update_mapper가_0건_변경을_반환하면_NotFoundException을_던진다() {
        service = new BusinessFacilityService(businessFacilityMapper, organizationMapper, facilityImageMapper);

        FacilityResponse existing = new FacilityResponse();
        existing.setId(5L);
        FacilityUpsertRequest request = validRequest();
        when(businessFacilityMapper.findByIdForOwner(5L, OWNER_ID)).thenReturn(existing);
        when(organizationMapper.countOwnedByUser(1L, OWNER_ID)).thenReturn(1);
        when(businessFacilityMapper.updateForOwner(5L, OWNER_ID, request)).thenReturn(0);

        assertThatThrownBy(() -> service.update(5L, OWNER_ID, request))
                .isInstanceOf(NotFoundException.class);
    }
}
