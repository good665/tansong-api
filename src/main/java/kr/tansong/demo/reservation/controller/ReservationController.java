package kr.tansong.demo.reservation.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.reservation.dto.ReservationRequest;
import kr.tansong.demo.reservation.service.ReservationService;

/**
 * userId 는 클라이언트가 보내지 않고 토큰에서 꺼냅니다.
 * (예외는 GlobalExceptionHandler 가 처리하고, CORS 는 SecurityConfig 가 처리합니다)
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<?> createReservation(
            @AuthenticationPrincipal Long userId,
            @RequestBody ReservationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("방문 예약 정보가 없습니다.");
        }

        request.setUserId(userId);   // 본문에 다른 userId 가 와도 무시하고 토큰 값으로 덮어씀

        int result = reservationService.createReservation(request);

        if (result == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "방문 예약이 접수되었습니다."));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "방문 예약을 저장하지 못했습니다."));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReservations(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(reservationService.getMyReservations(userId));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<?> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long userId) {

        boolean canceled = reservationService.cancelReservation(reservationId, userId);

        if (!canceled) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "방문 예약을 취소할 수 없습니다. 예약 상태를 확인해 주세요."));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "방문 예약이 취소되었습니다."));
    }
}
