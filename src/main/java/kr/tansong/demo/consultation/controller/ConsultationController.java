package kr.tansong.demo.consultation.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;


import kr.tansong.demo.consultation.dto.ConsultationRequest;
import kr.tansong.demo.consultation.service.ConsultationService;

@RestController
@RequestMapping("/api/consultations")
@CrossOrigin(origins = "*")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(
            ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    public ResponseEntity<?> createConsultation(
            @RequestBody ConsultationRequest request) {

        try {
            int result = consultationService.createConsultation(request);

            if (result == 1) {
                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(Map.of(
                                "success", true,
                                "message", "상담 신청이 접수되었습니다."
                        ));
            }

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "상담 신청을 저장하지 못했습니다."
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<?> getMyConsultations(
            @RequestParam Long userId) {

        try {
            return ResponseEntity.ok(
                consultationService.getMyConsultations(userId)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", e.getMessage()
                )
            );
        }
    }
    
    @PatchMapping("/{consultationId}/cancel")
    public ResponseEntity<?> cancelConsultation(
            @PathVariable Long consultationId,
            @RequestParam Long userId) {

        boolean canceled = consultationService.cancelConsultation(
            consultationId,
            userId
        );

        if (!canceled) {
            return ResponseEntity.badRequest().body(
                java.util.Map.of(
                    "success", false,
                    "message", "상담을 취소할 수 없습니다. 상담 상태를 확인해 주세요."
                )
            );
        }

        return ResponseEntity.ok(
            java.util.Map.of(
                "success", true,
                "message", "상담 신청이 취소되었습니다."
            )
        );
    }
}
