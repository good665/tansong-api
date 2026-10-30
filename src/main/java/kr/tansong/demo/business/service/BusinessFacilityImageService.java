package kr.tansong.demo.business.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.common.ImageStorageService;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.dto.FacilityImageResponse;
import kr.tansong.demo.facility.mapper.BusinessFacilityMapper;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;

/** 사업자가 본인 소유 시설의 사진만 올리고 지울 수 있도록 함 (AdminFacilityImageService 와 동일한 저장 로직,
 *  소유권 확인만 BusinessFacilityMapper#findByIdForOwner 로 다름) */
@Service
public class BusinessFacilityImageService {

    private final BusinessFacilityMapper businessFacilityMapper;
    private final FacilityImageMapper facilityImageMapper;
    private final ImageStorageService imageStorageService;

    public BusinessFacilityImageService(BusinessFacilityMapper businessFacilityMapper,
                                        FacilityImageMapper facilityImageMapper,
                                        ImageStorageService imageStorageService) {
        this.businessFacilityMapper = businessFacilityMapper;
        this.facilityImageMapper = facilityImageMapper;
        this.imageStorageService = imageStorageService;
    }

    public List<FacilityImageResponse> list(Long facilityId, Long ownerUserId) {
        requireOwnedFacility(facilityId, ownerUserId);
        return facilityImageMapper.findByFacilityId(facilityId);
    }

    @Transactional
    public FacilityImageResponse upload(Long facilityId, Long ownerUserId, MultipartFile file) {
        requireOwnedFacility(facilityId, ownerUserId);

        String imageUrl = imageStorageService.storeFacilityImage(facilityId, file);
        int sortOrder = facilityImageMapper.nextSortOrder(facilityId);

        FacilityImageResponse image = new FacilityImageResponse();
        image.setFacilityId(facilityId);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder);

        facilityImageMapper.insert(image);
        return image;
    }

    @Transactional
    public void delete(Long facilityId, Long ownerUserId, Long photoId) {
        requireOwnedFacility(facilityId, ownerUserId);

        String imageUrl = facilityImageMapper.findImageUrl(photoId, facilityId);
        if (imageUrl == null) {
            throw new NotFoundException("사진을 찾을 수 없습니다.");
        }

        facilityImageMapper.deleteByIdAndFacilityId(photoId, facilityId);
        imageStorageService.deleteQuietly(imageUrl);
    }

    private void requireOwnedFacility(Long facilityId, Long ownerUserId) {
        if (facilityId == null || facilityId <= 0
                || businessFacilityMapper.findByIdForOwner(facilityId, ownerUserId) == null) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
    }
}
