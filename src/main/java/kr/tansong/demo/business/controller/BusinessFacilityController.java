package kr.tansong.demo.business.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.FacilityUpsertRequest;
import kr.tansong.demo.business.service.BusinessFacilityService;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.facility.dto.FacilityResponse;

/** 사업자(시설 운영자) 본인 소유 시설 관리 (SecurityConfig 에서 /api/business/** 를 ROLE_BUSINESS_OWNER 로 제한) */
@RestController
@RequestMapping("/api/business/facilities")
public class BusinessFacilityController {

    private final BusinessFacilityService businessFacilityService;

    public BusinessFacilityController(BusinessFacilityService businessFacilityService) {
        this.businessFacilityService = businessFacilityService;
    }

    @GetMapping
    public PageResponse<FacilityResponse> list(
            @AuthenticationPrincipal Long ownerUserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return businessFacilityService.list(ownerUserId, status, page, size);
    }

    @GetMapping("/{id}")
    public FacilityResponse getById(@AuthenticationPrincipal Long ownerUserId, @PathVariable Long id) {
        return businessFacilityService.getById(id, ownerUserId);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal Long ownerUserId,
            @RequestBody FacilityUpsertRequest request) {

        Long id = businessFacilityService.create(ownerUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "id", id,
                "message", "시설이 등록되었습니다. 관리자 승인 후 공개됩니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long id,
            @RequestBody FacilityUpsertRequest request) {

        businessFacilityService.update(id, ownerUserId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "시설 정보가 수정되었습니다."));
    }
}
