package kr.tansong.demo.admin.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.AdminConsultationResponse;
import kr.tansong.demo.admin.dto.StatusUpdateRequest;
import kr.tansong.demo.admin.service.AdminConsultationService;
import kr.tansong.demo.common.PageResponse;

/** 상담 신청 관리 (관리자 전용) */
@RestController
@RequestMapping("/api/admin/consultations")
public class AdminConsultationController {

    private final AdminConsultationService adminConsultationService;

    public AdminConsultationController(AdminConsultationService adminConsultationService) {
        this.adminConsultationService = adminConsultationService;
    }

    @GetMapping
    public PageResponse<AdminConsultationResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return adminConsultationService.list(status, page, size);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        adminConsultationService.updateStatus(id, request == null ? null : request.getStatus());
        return ResponseEntity.ok(Map.of("success", true, "message", "상담 상태가 변경되었습니다."));
    }
}
