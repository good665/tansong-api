package kr.tansong.demo.admin.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import kr.tansong.demo.admin.dto.AdminConsultationResponse;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.common.Paging;
import kr.tansong.demo.consultation.mapper.ConsultationMapper;

@Service
public class AdminConsultationService {

    private static final Set<String> STATUSES =
            Set.of("REQUESTED", "CONFIRMED", "COMPLETED", "CANCELED");

    private final ConsultationMapper consultationMapper;

    public AdminConsultationService(ConsultationMapper consultationMapper) {
        this.consultationMapper = consultationMapper;
    }

    public PageResponse<AdminConsultationResponse> list(String status, int page, int size) {
        page = Paging.page(page);
        size = Paging.size(size);

        String normalizedStatus =
                (status == null || status.isBlank() || "ALL".equals(status)) ? null : status.trim();

        long total = consultationMapper.countAdmin(normalizedStatus);
        List<AdminConsultationResponse> items =
                consultationMapper.findAllAdmin(normalizedStatus, size, page * size);

        return new PageResponse<>(items, page, size, total);
    }

    public void updateStatus(Long consultationId, String status) {
        if (status == null || !STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "상태 값은 REQUESTED, CONFIRMED, COMPLETED, CANCELED 중 하나여야 합니다.");
        }
        if (consultationMapper.updateStatusAdmin(consultationId, status) == 0) {
            throw new NotFoundException("존재하지 않는 상담 신청입니다.");
        }
    }
}
