package kr.tansong.demo.business.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.admin.dto.FacilityUpsertRequest;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.common.Paging;
import kr.tansong.demo.facility.FacilityTypes;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.BusinessFacilityMapper;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.OrganizationMapper;

/** 사업자(시설 운영자)가 본인 소유 시설만 등록/수정할 수 있도록 하는 서비스.
 *  관리자와 달리 상태를 ACTIVE/INACTIVE 로 직접 바꿀 수 없음 — 등록은 항상 PENDING 으로 시작해
 *  관리자 승인(AdminFacilityController 의 activate)을 거쳐야 공개 목록에 노출됩니다. */
@Service
public class BusinessFacilityService {

    private final BusinessFacilityMapper businessFacilityMapper;
    private final OrganizationMapper organizationMapper;
    private final FacilityImageMapper facilityImageMapper;

    public BusinessFacilityService(BusinessFacilityMapper businessFacilityMapper,
                                   OrganizationMapper organizationMapper,
                                   FacilityImageMapper facilityImageMapper) {
        this.businessFacilityMapper = businessFacilityMapper;
        this.organizationMapper = organizationMapper;
        this.facilityImageMapper = facilityImageMapper;
    }

    public PageResponse<FacilityResponse> list(Long ownerUserId, String status, int page, int size) {
        page = Paging.page(page);
        size = Paging.size(size);

        String normalizedStatus =
                (status == null || status.isBlank() || "ALL".equals(status)) ? null : status.trim();

        long total = businessFacilityMapper.countByOwner(ownerUserId, normalizedStatus);
        List<FacilityResponse> items =
                businessFacilityMapper.findAllByOwner(ownerUserId, normalizedStatus, size, page * size);

        items.forEach(f -> f.setPhotos(facilityImageMapper.findImageUrlsByFacilityId(f.getId())));

        return new PageResponse<>(items, page, size, total);
    }

    public FacilityResponse getById(Long id, Long ownerUserId) {
        FacilityResponse facility = requireOwnedFacility(id, ownerUserId);
        facility.setPhotos(facilityImageMapper.findImageUrlsByFacilityId(facility.getId()));
        return facility;
    }

    @Transactional
    public Long create(Long ownerUserId, FacilityUpsertRequest request) {
        validate(request, ownerUserId);
        businessFacilityMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, Long ownerUserId, FacilityUpsertRequest request) {
        requireOwnedFacility(id, ownerUserId);
        validate(request, ownerUserId);

        if (businessFacilityMapper.updateForOwner(id, ownerUserId, request) == 0) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
    }

    private FacilityResponse requireOwnedFacility(Long id, Long ownerUserId) {
        FacilityResponse facility = businessFacilityMapper.findByIdForOwner(id, ownerUserId);

        if (facility == null) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }

        return facility;
    }

    private void validate(FacilityUpsertRequest request, Long ownerUserId) {
        if (request == null) {
            throw new IllegalArgumentException("시설 정보가 없습니다.");
        }
        if (request.getOrganizationId() == null
                || organizationMapper.countOwnedByUser(request.getOrganizationId(), ownerUserId) == 0) {
            throw new IllegalArgumentException("본인 소유의 운영 기관이 아닙니다.");
        }
        if (request.getFacilityType() == null || !FacilityTypes.VALID.contains(request.getFacilityType())) {
            throw new IllegalArgumentException("시설 종류가 올바르지 않습니다.");
        }
        if (isBlank(request.getName()) || request.getName().length() > 150) {
            throw new IllegalArgumentException("시설명을 1~150자로 입력해 주세요.");
        }
        if (isBlank(request.getAddress()) || request.getAddress().length() > 255) {
            throw new IllegalArgumentException("주소를 1~255자로 입력해 주세요.");
        }

        BigDecimal min = request.getMonthlyFeeMin();
        BigDecimal max = request.getMonthlyFeeMax();

        if (min != null && min.signum() < 0) {
            throw new IllegalArgumentException("최소 월 비용은 0 이상이어야 합니다.");
        }
        if (max != null && max.signum() < 0) {
            throw new IllegalArgumentException("최대 월 비용은 0 이상이어야 합니다.");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("최소 월 비용이 최대 월 비용보다 클 수 없습니다.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
