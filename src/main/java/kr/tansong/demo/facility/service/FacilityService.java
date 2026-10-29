package kr.tansong.demo.facility.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.FacilityImageMapper;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.facility.mapper.FacilitySearchMapper;

@Service
public class FacilityService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final int MAX_PAGE = 10_000;
    private static final int MAX_KEYWORD_LENGTH = 50;

    private final FacilityMapper facilityMapper;
    private final FacilitySearchMapper facilitySearchMapper;
    private final FacilityImageMapper facilityImageMapper;

    public FacilityService(FacilityMapper facilityMapper,
                           FacilitySearchMapper facilitySearchMapper,
                           FacilityImageMapper facilityImageMapper) {
        this.facilityMapper = facilityMapper;
        this.facilitySearchMapper = facilitySearchMapper;
        this.facilityImageMapper = facilityImageMapper;
    }

    public List<FacilityResponse> findAll(String type, String keyword) {
        List<FacilityResponse> items = facilityMapper.findAll(type, keyword);
        items.forEach(this::attachPhotos);
        return items;
    }

    public FacilityResponse getFacilityById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("시설 ID가 올바르지 않습니다.");
        }
        FacilityResponse facility = facilityMapper.findById(id);
        attachPhotos(facility);
        return facility;
    }

    /** 서버 쪽 검색 + 페이징 (page 는 0부터 시작) */
    public PageResponse<FacilityResponse> search(String type, String keyword, int page, int size) {

        page = Math.max(0, Math.min(page, MAX_PAGE));
        size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        String normalizedType =
                (type == null || type.isBlank() || "ALL".equals(type)) ? null : type.trim();

        String normalizedKeyword = null;
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            if (k.length() > MAX_KEYWORD_LENGTH) {
                k = k.substring(0, MAX_KEYWORD_LENGTH);
            }
            normalizedKeyword = escapeLike(k);
        }

        long total = facilitySearchMapper.count(normalizedType, normalizedKeyword);
        List<FacilityResponse> items =
                facilitySearchMapper.search(normalizedType, normalizedKeyword, size, page * size);
        items.forEach(this::attachPhotos);

        return new PageResponse<>(items, page, size, total);
    }

    private void attachPhotos(FacilityResponse facility) {
        if (facility == null) {
            return;
        }
        facility.setPhotos(facilityImageMapper.findImageUrlsByFacilityId(facility.getId()));
    }

    // 사용자가 입력한 % _ 가 와일드카드로 동작하지 않게 이스케이프
    private String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}