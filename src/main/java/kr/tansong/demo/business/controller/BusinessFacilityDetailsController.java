package kr.tansong.demo.business.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.facilitydetail.dto.FacilityDetailsRequest;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse;
import kr.tansong.demo.facilitydetail.service.FacilityDetailsService;

/** 사업자 본인 소유 시설의 방 종류·가격·도면도 관리 (/api/business/** 는 ROLE_BUSINESS_OWNER 전용) */
@RestController
@RequestMapping("/api/business/facilities/{facilityId}/details")
public class BusinessFacilityDetailsController {

    private final FacilityDetailsService facilityDetailsService;

    public BusinessFacilityDetailsController(FacilityDetailsService facilityDetailsService) {
        this.facilityDetailsService = facilityDetailsService;
    }

    @GetMapping
    public FacilityDetailsResponse get(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        return facilityDetailsService.getForOwner(facilityId, ownerUserId);
    }

    @PutMapping
    public FacilityDetailsResponse replace(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId,
            @RequestBody FacilityDetailsRequest request) {

        return facilityDetailsService.replace(facilityId, ownerUserId, request);
    }

    @PostMapping("/floor-plans")
    public ResponseEntity<FacilityDetailsResponse.FloorPlan> addFloorPlan(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String label) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facilityDetailsService.addFloorPlan(facilityId, ownerUserId, file, label));
    }

    @DeleteMapping("/floor-plans/{planId}")
    public ResponseEntity<?> deleteFloorPlan(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId,
            @PathVariable Long planId) {

        facilityDetailsService.deleteFloorPlan(facilityId, ownerUserId, planId);
        return ResponseEntity.ok(Map.of("success", true, "message", "도면도가 삭제되었습니다."));
    }
}
