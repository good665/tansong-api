package kr.tansong.demo.consultation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
