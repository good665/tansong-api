package kr.tansong.demo.admin.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import kr.tansong.demo.admin.dto.AdminReservationResponse;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.common.Paging;
import kr.tansong.demo.reservation.mapper.ReservationMapper;

@Service
public class AdminReservationService {

    private static final Set<String> STATUSES =
            Set.of("REQUESTED", "CONFIRMED", "COMPLETED", "CANCELED");

    private final ReservationMapper reservationMapper;

    public AdminReservationService(ReservationMapper reservationMapper) {
        this.reservationMapper = reservationMapper;
    }

    public PageResponse<AdminReservationResponse> list(String status, int page, int size) {
        page = Paging.page(page);
        size = Paging.size(size);

        String normalizedStatus =
                (status == null || status.isBlank() || "ALL".equals(status)) ? null : status.trim();

        long total = reservationMapper.countAdmin(normalizedStatus);
        List<AdminReservationResponse> items =
                reservationMapper.findAllAdmin(normalizedStatus, size, page * size);

        return new PageResponse<>(items, page, size, total);
    }

    public void updateStatus(Long reservationId, String status) {
        if (status == null || !STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "상태 값은 REQUESTED, CONFIRMED, COMPLETED, CANCELED 중 하나여야 합니다.");
        }
        if (reservationMapper.updateStatusAdmin(reservationId, status) == 0) {
            throw new NotFoundException("존재하지 않는 방문 예약입니다.");
        }
    }
}
