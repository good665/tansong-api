package kr.tansong.demo.business.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.business.service.BusinessFacilityImageService;
import kr.tansong.demo.facility.dto.FacilityImageResponse;

/** 사업자 본인 소유 시설의 사진 관리 (SecurityConfig 에서 /api/business/** 를 ROLE_BUSINESS_OWNER 로 제한) */
@RestController
@RequestMapping("/api/business/facilities/{facilityId}/photos")
public class BusinessFacilityImageController {

    private final BusinessFacilityImageService businessFacilityImageService;

    public BusinessFacilityImageController(BusinessFacilityImageService businessFacilityImageService) {
        this.businessFacilityImageService = businessFacilityImageService;
    }

    @GetMapping
    public List<FacilityImageResponse> list(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId) {

        return businessFacilityImageService.list(facilityId, ownerUserId);
    }

    @PostMapping
    public ResponseEntity<FacilityImageResponse> upload(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId,
            @RequestParam("file") MultipartFile file) {

        FacilityImageResponse image = businessFacilityImageService.upload(facilityId, ownerUserId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(image);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long facilityId,
            @PathVariable Long photoId) {

        businessFacilityImageService.delete(facilityId, ownerUserId, photoId);
        return ResponseEntity.ok(Map.of("success", true, "message", "사진이 삭제되었습니다."));
    }
}
