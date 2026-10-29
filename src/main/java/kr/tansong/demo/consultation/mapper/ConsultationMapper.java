package kr.tansong.demo.consultation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.tansong.demo.admin.dto.AdminConsultationResponse;
import kr.tansong.demo.consultation.dto.ConsultationHistoryResponse;
import kr.tansong.demo.consultation.dto.ConsultationRequest;


@Mapper
public interface ConsultationMapper {
	// 상담 신청 저장
    int insert(@Param("request") ConsultationRequest request);

    // 사용자별 상담 신청 목록 조회
    List<ConsultationHistoryResponse> findByUserId(
        @Param("userId") Long userId
    );
    // 상담 신청 취소 요청
    int cancelConsultation(
    	    @Param("consultationId") Long consultationId,
    	    @Param("userId") Long userId
    	);

    // 관리자: 전체 상담 신청 목록 (status 는 선택적 필터)
    List<AdminConsultationResponse> findAllAdmin(
            @Param("status") String status,
            @Param("size") int size,
            @Param("offset") int offset);

    long countAdmin(@Param("status") String status);

    // 관리자: 상담 상태 변경 (사용자 본인 여부와 무관)
    int updateStatusAdmin(@Param("consultationId") Long consultationId, @Param("status") String status);
}
