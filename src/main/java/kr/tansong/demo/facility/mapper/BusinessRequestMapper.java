package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.tansong.demo.business.dto.BusinessRequestResponse;

/** 사업자 본인 시설의 상담·방문 예약. 모든 쿼리는 organizations.owner_user_id 로 범위를 제한함 */
@Mapper
public interface BusinessRequestMapper {

    @Select("""
        <script>
        SELECT c.id, c.facility_id AS facilityId, f.name AS facilityName,
               u.name AS userName, u.phone AS userPhone,
               DATE_FORMAT(c.preferred_at, '%Y-%m-%d %H:%i:%s') AS requestedAt,
               c.consultation_method AS consultationMethod, c.message, c.status,
               DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
          FROM consultations c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = f.organization_id
          JOIN users u ON u.id = c.user_id
         WHERE o.owner_user_id = #{ownerUserId}
         <if test="status != null">AND c.status = #{status}</if>
         ORDER BY c.created_at DESC, c.id DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<BusinessRequestResponse> findConsultations(@Param("ownerUserId") Long ownerUserId,
                                                    @Param("status") String status,
                                                    @Param("size") int size,
                                                    @Param("offset") int offset);

    @Select("""
        <script>
        SELECT COUNT(*)
          FROM consultations c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = f.organization_id
         WHERE o.owner_user_id = #{ownerUserId}
         <if test="status != null">AND c.status = #{status}</if>
        </script>
        """)
    long countConsultations(@Param("ownerUserId") Long ownerUserId, @Param("status") String status);

    @Select("""
        SELECT c.status
          FROM consultations c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = f.organization_id
         WHERE c.id = #{id} AND o.owner_user_id = #{ownerUserId}
        """)
    String findConsultationStatus(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    // from 조건: 조회와 변경 사이에 고객이 취소했다면 덮어쓰지 않음
    @Update("""
        UPDATE consultations c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = f.organization_id
           SET c.status = #{to}
         WHERE c.id = #{id} AND o.owner_user_id = #{ownerUserId} AND c.status = #{from}
        """)
    int updateConsultationStatus(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId,
                                 @Param("from") String from, @Param("to") String to);

    @Select("""
        <script>
        SELECT r.id, r.facility_id AS facilityId, f.name AS facilityName,
               u.name AS userName, u.phone AS userPhone,
               DATE_FORMAT(r.scheduled_at, '%Y-%m-%d %H:%i:%s') AS requestedAt,
               r.request_message AS message, r.status,
               DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
          FROM reservations r
          JOIN facilities f ON f.id = r.facility_id
          JOIN organizations o ON o.id = f.organization_id
          JOIN users u ON u.id = r.user_id
         WHERE o.owner_user_id = #{ownerUserId}
         <if test="status != null">AND r.status = #{status}</if>
         ORDER BY r.created_at DESC, r.id DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<BusinessRequestResponse> findReservations(@Param("ownerUserId") Long ownerUserId,
                                                   @Param("status") String status,
                                                   @Param("size") int size,
                                                   @Param("offset") int offset);

    @Select("""
        <script>
        SELECT COUNT(*)
          FROM reservations r
          JOIN facilities f ON f.id = r.facility_id
          JOIN organizations o ON o.id = f.organization_id
         WHERE o.owner_user_id = #{ownerUserId}
         <if test="status != null">AND r.status = #{status}</if>
        </script>
        """)
    long countReservations(@Param("ownerUserId") Long ownerUserId, @Param("status") String status);

    @Select("""
        SELECT r.status
          FROM reservations r
          JOIN facilities f ON f.id = r.facility_id
          JOIN organizations o ON o.id = f.organization_id
         WHERE r.id = #{id} AND o.owner_user_id = #{ownerUserId}
        """)
    String findReservationStatus(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    @Update("""
        UPDATE reservations r
          JOIN facilities f ON f.id = r.facility_id
          JOIN organizations o ON o.id = f.organization_id
           SET r.status = #{to}
         WHERE r.id = #{id} AND o.owner_user_id = #{ownerUserId} AND r.status = #{from}
        """)
    int updateReservationStatus(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId,
                                @Param("from") String from, @Param("to") String to);
}
