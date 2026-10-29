package kr.tansong.demo.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.OrganizationCreateRequest;
import kr.tansong.demo.admin.dto.OrganizationSummaryResponse;
import kr.tansong.demo.admin.service.AdminOrganizationService;

/** 시설 등록/수정 화면의 운영 기관 선택·등록용 (관리자 전용, SecurityConfig 에서 /api/admin/** 를 ROLE_ADMIN 으로 제한) */
@RestController
@RequestMapping("/api/admin/organizations")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    public AdminOrganizationController(AdminOrganizationService adminOrganizationService) {
        this.adminOrganizationService = adminOrganizationService;
    }

    @GetMapping
    public List<OrganizationSummaryResponse> list() {
        return adminOrganizationService.list();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrganizationCreateRequest request) {
        Long id = adminOrganizationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "id", id,
                "message", "운영 기관이 등록되었습니다."));
    }
}
