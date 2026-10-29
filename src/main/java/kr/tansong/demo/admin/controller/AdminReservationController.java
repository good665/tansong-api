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

import kr.tansong.demo.admin.dto.AdminReservationResponse;
import kr.tansong.demo.admin.dto.StatusUpdateRequest;
import kr.tansong.demo.admin.service.AdminReservationService;
import kr.tansong.demo.common.PageResponse;

/** 방문 예약 관리 (관리자 전용) */
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    public AdminReservationController(AdminReservationService adminReservationService) {
        this.adminReservationService = adminReservationService;
    }

    @GetMapping
    public PageResponse<AdminReservationResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return adminReservationService.list(status, page, size);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        adminReservationService.updateStatus(id, request == null ? null : request.getStatus());
        return ResponseEntity.ok(Map.of("success", true, "message", "방문 예약 상태가 변경되었습니다."));
    }
}
