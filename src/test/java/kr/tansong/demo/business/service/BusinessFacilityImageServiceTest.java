package kr.tansong.demo.business.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import kr.tansong.demo.common.ImageStorageService;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.BusinessFacilityMapper;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;

/** 사업자 시설 사진은 앱에서 줄여 올리며, 서버도 50KB 를 넘으면 받지 않음 */
@ExtendWith(MockitoExtension.class)
class BusinessFacilityImageServiceTest {

    private static final int LIMIT = 50 * 1024;

    @Mock
    private BusinessFacilityMapper businessFacilityMapper;
    @Mock
    private FacilityImageMapper facilityImageMapper;
    @Mock
    private ImageStorageService storage;

    private BusinessFacilityImageService service;

    @BeforeEach
    void setUp() {
        service = new BusinessFacilityImageService(businessFacilityMapper, facilityImageMapper, storage);
        when(businessFacilityMapper.findByIdForOwner(9L, 1L)).thenReturn(new FacilityResponse());
    }

    private static MockMultipartFile fileOf(int bytes) {
        return new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[bytes]);
    }

    @Test
    void 정확히_50KB는_저장한다() {
        when(storage.storeFacilityImage(any(), any())).thenReturn("/uploads/a.jpg");

        service.upload(9L, 1L, fileOf(LIMIT));

        verify(storage).storeFacilityImage(any(), any());
    }

    @Test
    void 제한을_넘으면_저장하지_않고_거절한다() {
        assertThatThrownBy(() -> service.upload(9L, 1L, fileOf(LIMIT + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50KB");

        verify(storage, never()).storeFacilityImage(any(), any());
    }
}
