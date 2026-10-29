package kr.tansong.demo.facility.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.service.FacilityService;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    // 기존 전체 목록 (하위 호환용)
    @GetMapping
    public List<FacilityResponse> findAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {

        return facilityService.findAll(type, keyword);
    }

    // 검색 + 페이징: GET /api/facilities/search?type=&keyword=&page=0&size=10
    @GetMapping("/search")
    public PageResponse<FacilityResponse> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return facilityService.search(type, keyword, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFacilityById(@PathVariable Long id) {
        FacilityResponse facility = facilityService.getFacilityById(id);

        if (facility == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(facility);
    }
}