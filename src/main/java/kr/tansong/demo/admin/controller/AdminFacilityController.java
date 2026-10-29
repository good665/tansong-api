package kr.tansong.demo.admin.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.FacilityUpsertRequest;
import kr.tansong.demo.admin.service.AdminFacilityService;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.facility.dto.FacilityResponse;

/** 시설 관리 (관리자 전용, SecurityConfig 에서 /api/admin/** 를 ROLE_ADMIN 으로 제한) */
@RestController
@RequestMapping("/api/admin/facilities")
public class AdminFacilityController {

    private final AdminFacilityService adminFacilityService;

    public AdminFacilityController(AdminFacilityService adminFacilityService) {
        this.adminFacilityService = adminFacilityService;
    }

    // 운영 중지된 시설도 포함해서 조회: GET /api/admin/facilities?status=&page=0&size=10
    @GetMapping
    public PageResponse<FacilityResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return adminFacilityService.list(status, page, size);
    }

    // 상태(ACTIVE/INACTIVE)와 무관하게 단건 조회 (수정 화면용)
    @GetMapping("/{id}")
    public FacilityResponse getById(@PathVariable Long id) {
        return adminFacilityService.getById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FacilityUpsertRequest request) {
        Long id = adminFacilityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "id", id,
                "message", "시설이 등록되었습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FacilityUpsertRequest request) {
        adminFacilityService.update(id, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "시설 정보가 수정되었습니다."));
    }

    // 실제 삭제가 아니라 status 를 INACTIVE 로 바꿔 공개 목록에서만 숨깁니다.
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        adminFacilityService.setStatus(id, "INACTIVE");
        return ResponseEntity.ok(Map.of("success", true, "message", "시설이 비활성화되었습니다."));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        adminFacilityService.setStatus(id, "ACTIVE");
        return ResponseEntity.ok(Map.of("success", true, "message", "시설이 다시 활성화되었습니다."));
    }
}
