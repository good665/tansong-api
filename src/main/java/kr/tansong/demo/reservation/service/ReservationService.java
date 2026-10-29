package kr.tansong.demo.reservation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.facility.mapper.FacilityMapper;
import kr.tansong.demo.reservation.dto.ReservationRequest;
import kr.tansong.demo.reservation.dto.ReservationResponse;
import kr.tansong.demo.reservation.mapper.ReservationMapper;

@Service
public class ReservationService {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final ReservationMapper reservationMapper;
    private final FacilityMapper facilityMapper;
    private final int slotCapacity;

    public ReservationService(ReservationMapper reservationMapper,
                              FacilityMapper facilityMapper,
                              @Value("${app.reservation.slot-capacity:2}") int slotCapacity) {
        this.reservationMapper = reservationMapper;
        this.facilityMapper = facilityMapper;
        this.slotCapacity = slotCapacity;
    }

    @Transactional
    public int createReservation(ReservationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("방문 예약 정보가 없습니다.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("사용자 ID가 올바르지 않습니다.");
        }
        if (request.getFacilityId() == null || request.getFacilityId() <= 0) {
            throw new IllegalArgumentException("시설 ID가 올바르지 않습니다.");
        }

        // 존재하는(운영 중인) 시설인지 확인
        if (facilityMapper.findById(request.getFacilityId()) == null) {
            throw new IllegalArgumentException("존재하지 않는 시설입니다.");
        }

        String scheduledAt = normalizeScheduledAt(request.getScheduledAt());
        request.setScheduledAt(scheduledAt);

        if (request.getMessage() != null && request.getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("요청 사항은 " + MAX_MESSAGE_LENGTH + "자 이하로 입력해 주세요.");
        }

        // 같은 시설, 같은 시간대의 방문 예약 정원 확인
        int activeCount = reservationMapper.countActiveByFacilityAndTime(
                request.getFacilityId(), scheduledAt);

        if (activeCount >= slotCapacity) {
            throw new ConflictException("해당 시간대는 방문 예약이 모두 찼습니다. 다른 시간을 선택해 주세요.");
        }

        return reservationMapper.insert(request);
    }

    public List<ReservationResponse> getMyReservations(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID가 올바르지 않습니다.");
        }
        return reservationMapper.findByUserId(userId);
    }

    public boolean cancelReservation(Long reservationId, Long userId) {
        return reservationMapper.cancelReservation(reservationId, userId) == 1;
    }

    private String normalizeScheduledAt(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isBlank()) {
            throw new IllegalArgumentException("희망 방문 날짜와 시간을 입력해 주세요.");
        }

        LocalDateTime parsed;

        try {
            parsed = LocalDateTime.parse(scheduledAt);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "방문 희망 시간 형식이 올바르지 않습니다. 예: 2026-10-26T14:00:00");
        }

        if (parsed.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("현재 시각 이후로 예약해 주세요.");
        }

        return scheduledAt;
    }
}
