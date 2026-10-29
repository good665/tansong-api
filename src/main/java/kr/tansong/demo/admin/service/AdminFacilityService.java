package kr.tansong.demo.admin.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.admin.dto.FacilityUpsertRequest;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.common.Paging;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.AdminFacilityMapper;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.FacilityMapper;

@Service
public class AdminFacilityService {

    private static final Set<String> FACILITY_TYPES = Set.of(
            "SILVER_TOWN", "NURSING_HOME", "NURSING_HOSPITAL", "DAY_CARE", "HOME_CARE");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final AdminFacilityMapper adminFacilityMapper;
    private final FacilityMapper facilityMapper;
    private final FacilityImageMapper facilityImageMapper;

    public AdminFacilityService(AdminFacilityMapper adminFacilityMapper,
                                FacilityMapper facilityMapper,
                                FacilityImageMapper facilityImageMapper) {
        this.adminFacilityMapper = adminFacilityMapper;
        this.facilityMapper = facilityMapper;
        this.facilityImageMapper = facilityImageMapper;
    }

    public PageResponse<FacilityResponse> list(String status, int page, int size) {
        page = Paging.page(page);
        size = Paging.size(size);

        String normalizedStatus =
                (status == null || status.isBlank() || "ALL".equals(status)) ? null : status.trim();

        long total = adminFacilityMapper.countAdmin(normalizedStatus);
        List<FacilityResponse> items =
                adminFacilityMapper.findAllAdmin(normalizedStatus, size, page * size);

        items.forEach(f -> f.setPhotos(facilityImageMapper.findImageUrlsByFacilityId(f.getId())));

        return new PageResponse<>(items, page, size, total);
    }

    public FacilityResponse getById(Long id) {
        FacilityResponse facility = adminFacilityMapper.findByIdAdmin(id);

        if (facility == null) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }

        facility.setPhotos(facilityImageMapper.findImageUrlsByFacilityId(facility.getId()));
        return facility;
    }

    @Transactional
    public Long create(FacilityUpsertRequest request) {
        validate(request);
        adminFacilityMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, FacilityUpsertRequest request) {
        requireFacility(id);
        validate(request);

        if (adminFacilityMapper.update(id, request) == 0) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
    }

    @Transactional
    public void setStatus(Long id, String status) {
        requireFacility(id);

        if (status == null || !STATUSES.contains(status)) {
            throw new IllegalArgumentException("상태 값은 ACTIVE 또는 INACTIVE 여야 합니다.");
        }

        adminFacilityMapper.updateStatus(id, status);
    }

    private void validate(FacilityUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("시설 정보가 없습니다.");
        }
        if (request.getOrganizationId() == null
                || adminFacilityMapper.countOrganizationById(request.getOrganizationId()) == 0) {
            throw new IllegalArgumentException("존재하지 않는 운영 기관입니다.");
        }
        if (request.getFacilityType() == null || !FACILITY_TYPES.contains(request.getFacilityType())) {
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

    private void requireFacility(Long id) {
        if (id == null || id <= 0 || facilityMapper.countById(id) == 0) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
