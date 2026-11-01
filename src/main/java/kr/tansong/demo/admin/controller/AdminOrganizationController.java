package kr.tansong.demo.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.OrganizationCreateRequest;
import kr.tansong.demo.admin.dto.OrganizationSummaryResponse;
import kr.tansong.demo.admin.service.AdminOrganizationService;
import kr.tansong.demo.verification.BusinessVerificationService;
import kr.tansong.demo.verification.VerificationResult;

/** 시설 등록/수정 화면의 운영 기관 선택·등록용 (관리자 전용, SecurityConfig 에서 /api/admin/** 를 ROLE_ADMIN 으로 제한) */
@RestController
@RequestMapping("/api/admin/organizations")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;
    private final BusinessVerificationService businessVerificationService;

    public AdminOrganizationController(AdminOrganizationService adminOrganizationService,
                                       BusinessVerificationService businessVerificationService) {
        this.adminOrganizationService = adminOrganizationService;
        this.businessVerificationService = businessVerificationService;
    }

    // 국세청 진위확인 재실행 (가입 때 API 장애로 "미확인"이 된 기관용)
    @PostMapping("/{id}/business-verification")
    public Map<String, Object> reverify(@PathVariable Long id) {
        VerificationResult result = businessVerificationService.reverify(id);
        return Map.of("status", result.status().name(), "message", result.message());
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
