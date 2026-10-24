package kr.tansong.demo.consultation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.consultation.dto.ConsultationHistoryResponse;
import kr.tansong.demo.consultation.dto.ConsultationRequest;
import kr.tansong.demo.consultation.mapper.ConsultationMapper;
import kr.tansong.demo.facility.mapper.FacilityMapper;

@Service
public class ConsultationService {

    private final ConsultationMapper consultationMapper;
    private final FacilityMapper facilityMapper;

    public ConsultationService(ConsultationMapper consultationMapper,
                               FacilityMapper facilityMapper) {
        this.consultationMapper = consultationMapper;
        this.facilityMapper = facilityMapper;
    }

    @Transactional
    public int createConsultation(ConsultationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("상담 신청 정보가 없습니다.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("사용자 ID가 올바르지 않습니다.");
        }
        if (request.getFacilityId() == null || request.getFacilityId() <= 0) {
            throw new IllegalArgumentException("시설 ID가 올바르지 않습니다.");
        }

        // 존재하는 시설인지 확인
        if (facilityMapper.findById(request.getFacilityId()) == null) {
            throw new IllegalArgumentException("존재하지 않는 시설입니다.");
        }

        String method = request.getConsultationMethod();
        if (method == null || !(method.equals("PHONE") || method.equals("VISIT"))) {
            throw new IllegalArgumentException("상담 방법은 PHONE 또는 VISIT이어야 합니다.");
        }

        validatePreferredAt(request.getPreferredAt());

        // 문의 내용 길이 제한
        if (request.getMessage() != null && request.getMessage().length() > 2000) {
            throw new IllegalArgumentException("문의 내용은 2000자 이하로 입력해 주세요.");
        }

        return consultationMapper.insert(request);
    }

    public List<ConsultationHistoryResponse> getMyConsultations(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID가 올바르지 않습니다.");
        }
        return consultationMapper.findByUserId(userId);
    }

    // Mapper가 반환한 변경 행 수로 취소 성공 여부 판단
    public boolean cancelConsultation(Long consultationId, Long userId) {
        return consultationMapper.cancelConsultation(consultationId, userId) == 1;
    }

    private void validatePreferredAt(String preferredAt) {
        if (preferredAt == null || preferredAt.isBlank()) {
            return;
        }

        LocalDateTime parsed;

        try {
            parsed = LocalDateTime.parse(preferredAt);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "희망 상담 시간 형식이 올바르지 않습니다. 예: 2026-10-26T14:00:00");
        }

        // 서버에서도 과거 시간 차단
        if (parsed.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("현재 시각 이후로 신청해 주세요.");
        }
    }
}