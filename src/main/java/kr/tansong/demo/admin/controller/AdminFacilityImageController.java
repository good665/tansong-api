package kr.tansong.demo.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.admin.service.AdminFacilityImageService;
import kr.tansong.demo.facility.dto.FacilityImageResponse;

/** 시설 사진 관리 (관리자 전용, SecurityConfig 에서 /api/admin/** 를 ROLE_ADMIN 으로 제한) */
@RestController
@RequestMapping("/api/admin/facilities/{facilityId}/photos")
public class AdminFacilityImageController {

    private final AdminFacilityImageService adminFacilityImageService;

    public AdminFacilityImageController(AdminFacilityImageService adminFacilityImageService) {
        this.adminFacilityImageService = adminFacilityImageService;
    }

    @GetMapping
    public List<FacilityImageResponse> list(@PathVariable Long facilityId) {
        return adminFacilityImageService.list(facilityId);
    }

    @PostMapping
    public ResponseEntity<FacilityImageResponse> upload(
            @PathVariable Long facilityId,
            @RequestParam("file") MultipartFile file) {

        FacilityImageResponse image = adminFacilityImageService.upload(facilityId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(image);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> delete(@PathVariable Long facilityId, @PathVariable Long photoId) {
        adminFacilityImageService.delete(facilityId, photoId);
        return ResponseEntity.ok(Map.of("success", true, "message", "사진이 삭제되었습니다."));
    }
}
