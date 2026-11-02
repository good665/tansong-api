package kr.tansong.demo.business.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.StatusUpdateRequest;
import kr.tansong.demo.business.dto.BusinessRequestResponse;
import kr.tansong.demo.business.service.BusinessRequestService;
import kr.tansong.demo.common.PageResponse;

/** 사업자 본인 시설의 상담·방문 예약 (/api/business/** 는 ROLE_BUSINESS_OWNER 전용) */
@RestController
@RequestMapping("/api/business")
public class BusinessRequestController {

    private final BusinessRequestService service;

    public BusinessRequestController(BusinessRequestService service) {
        this.service = service;
    }

    @GetMapping("/consultations")
    public PageResponse<BusinessRequestResponse> consultations(
            @AuthenticationPrincipal Long ownerUserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.listConsultations(ownerUserId, status, page, size);
    }

    @PatchMapping("/consultations/{id}/status")
    public ResponseEntity<?> updateConsultation(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {

        service.updateConsultationStatus(id, ownerUserId, request == null ? null : request.getStatus());
        return ResponseEntity.ok(Map.of("success", true, "message", "상담 상태가 변경되었습니다."));
    }

    @GetMapping("/reservations")
    public PageResponse<BusinessRequestResponse> reservations(
            @AuthenticationPrincipal Long ownerUserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.listReservations(ownerUserId, status, page, size);
    }

    @PatchMapping("/reservations/{id}/status")
    public ResponseEntity<?> updateReservation(
            @AuthenticationPrincipal Long ownerUserId,
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {

        service.updateReservationStatus(id, ownerUserId, request == null ? null : request.getStatus());
        return ResponseEntity.ok(Map.of("success", true, "message", "방문 예약 상태가 변경되었습니다."));
    }
}
