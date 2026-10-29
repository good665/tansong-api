package kr.tansong.demo.reservation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.tansong.demo.admin.dto.AdminReservationResponse;
import kr.tansong.demo.reservation.dto.ReservationRequest;
import kr.tansong.demo.reservation.dto.ReservationResponse;

@Mapper
public interface ReservationMapper {

    // 같은 시설, 같은 시간대의 활성 예약 수 (REQUESTED/CONFIRMED) - 슬롯 정원 확인용
    int countActiveByFacilityAndTime(
            @Param("facilityId") Long facilityId,
            @Param("scheduledAt") String scheduledAt);

    int insert(@Param("request") ReservationRequest request);

    List<ReservationResponse> findByUserId(@Param("userId") Long userId);

    int cancelReservation(
            @Param("reservationId") Long reservationId,
            @Param("userId") Long userId);

    // 관리자: 전체 방문 예약 목록 (status 는 선택적 필터)
    List<AdminReservationResponse> findAllAdmin(
            @Param("status") String status,
            @Param("size") int size,
            @Param("offset") int offset);

    long countAdmin(@Param("status") String status);

    // 관리자: 예약 상태 변경 (사용자 본인 여부와 무관)
    int updateStatusAdmin(@Param("reservationId") Long reservationId, @Param("status") String status);
}
