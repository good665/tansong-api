package kr.tansong.demo.business.service;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import kr.tansong.demo.business.dto.BusinessRequestResponse;
import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.common.Paging;
import kr.tansong.demo.facility.mapper.BusinessRequestMapper;

/** 사업자가 본인 시설의 상담·방문 예약을 보고 처리함 */
@Service
public class BusinessRequestService {

    private static final Map<String, Set<String>> NEXT_STATUSES = Map.of(
            "REQUESTED", Set.of("CONFIRMED", "CANCELED"),
            "CONFIRMED", Set.of("COMPLETED", "CANCELED"));

    private final BusinessRequestMapper mapper;

    public BusinessRequestService(BusinessRequestMapper mapper) {
        this.mapper = mapper;
    }

    public PageResponse<BusinessRequestResponse> listConsultations(Long ownerUserId, String status, int page, int size) {
        int p = Paging.page(page);
        int s = Paging.size(size);
        String filter = normalize(status);
        return new PageResponse<>(mapper.findConsultations(ownerUserId, filter, s, p * s), p, s,
                mapper.countConsultations(ownerUserId, filter));
    }

    public PageResponse<BusinessRequestResponse> listReservations(Long ownerUserId, String status, int page, int size) {
        int p = Paging.page(page);
        int s = Paging.size(size);
        String filter = normalize(status);
        return new PageResponse<>(mapper.findReservations(ownerUserId, filter, s, p * s), p, s,
                mapper.countReservations(ownerUserId, filter));
    }

    public void updateConsultationStatus(Long id, Long ownerUserId, String to) {
        String from = mapper.findConsultationStatus(id, ownerUserId);
        if (from == null) {
            throw new NotFoundException("존재하지 않는 상담 신청입니다.");
        }
        requireAllowed(from, to);
        requireUpdated(mapper.updateConsultationStatus(id, ownerUserId, from, to));
    }

    public void updateReservationStatus(Long id, Long ownerUserId, String to) {
        String from = mapper.findReservationStatus(id, ownerUserId);
        if (from == null) {
            throw new NotFoundException("존재하지 않는 방문 예약입니다.");
        }
        requireAllowed(from, to);
        requireUpdated(mapper.updateReservationStatus(id, ownerUserId, from, to));
    }

    private static String normalize(String status) {
        return status == null || status.isBlank() || "ALL".equals(status) ? null : status.trim();
    }

    private static void requireAllowed(String from, String to) {
        if (to == null || !NEXT_STATUSES.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("현재 상태에서는 변경할 수 없습니다.");
        }
    }

    private static void requireUpdated(int rows) {
        if (rows == 0) {
            throw new ConflictException("상태가 이미 변경되었습니다. 목록을 새로고침해 주세요.");
        }
    }
}
