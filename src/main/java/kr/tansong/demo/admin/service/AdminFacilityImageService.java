package kr.tansong.demo.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.common.ImageStorageService;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.dto.FacilityImageResponse;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.FacilityMapper;

@Service
public class AdminFacilityImageService {

    private final FacilityMapper facilityMapper;
    private final FacilityImageMapper facilityImageMapper;
    private final ImageStorageService imageStorageService;

    public AdminFacilityImageService(FacilityMapper facilityMapper,
                                     FacilityImageMapper facilityImageMapper,
                                     ImageStorageService imageStorageService) {
        this.facilityMapper = facilityMapper;
        this.facilityImageMapper = facilityImageMapper;
        this.imageStorageService = imageStorageService;
    }

    public List<FacilityImageResponse> list(Long facilityId) {
        requireFacility(facilityId);
        return facilityImageMapper.findByFacilityId(facilityId);
    }

    @Transactional
    public FacilityImageResponse upload(Long facilityId, MultipartFile file) {
        requireFacility(facilityId);

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
    public void delete(Long facilityId, Long photoId) {
        requireFacility(facilityId);

        String imageUrl = facilityImageMapper.findImageUrl(photoId, facilityId);
        if (imageUrl == null) {
            throw new NotFoundException("사진을 찾을 수 없습니다.");
        }

        facilityImageMapper.deleteByIdAndFacilityId(photoId, facilityId);
        imageStorageService.deleteQuietly(imageUrl);
    }

    private void requireFacility(Long facilityId) {
        if (facilityId == null || facilityId <= 0 || facilityMapper.countById(facilityId) == 0) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
    }
}
