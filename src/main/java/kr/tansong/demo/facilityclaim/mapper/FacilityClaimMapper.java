package kr.tansong.demo.facilityclaim.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.tansong.demo.facilityclaim.dto.ClaimableFacilityResponse;
import kr.tansong.demo.facilityclaim.dto.FacilityClaimResponse;

/** 시설 소유권 신청(facility_claims)과 승인 시 소유권 이전 */
@Mapper
public interface FacilityClaimMapper {

    @Select("""
        SELECT f.id, f.name, f.address, f.phone, f.facility_type AS facilityType,
               EXISTS (SELECT 1 FROM facility_claims c
                        WHERE c.facility_id = f.id AND c.requester_user_id = #{ownerUserId} AND c.status = 'PENDING') AS myPending
          FROM facilities f
          JOIN public_facility_sources s ON s.facility_id = f.id AND s.claimed_at IS NULL
         WHERE f.status = 'ACTIVE'
           AND (f.name LIKE CONCAT('%', #{keyword}, '%') OR f.address LIKE CONCAT('%', #{keyword}, '%'))
         ORDER BY f.name, f.id
         LIMIT #{size} OFFSET #{offset}
        """)
    List<ClaimableFacilityResponse> findClaimable(@Param("keyword") String keyword, @Param("ownerUserId") Long ownerUserId,
                                                  @Param("size") int size, @Param("offset") int offset);

    @Select("""
        SELECT COUNT(*)
          FROM facilities f
          JOIN public_facility_sources s ON s.facility_id = f.id AND s.claimed_at IS NULL
         WHERE f.status = 'ACTIVE'
           AND (f.name LIKE CONCAT('%', #{keyword}, '%') OR f.address LIKE CONCAT('%', #{keyword}, '%'))
        """)
    long countClaimable(@Param("keyword") String keyword);

    // 시설 행을 잠가 동시 신청·승인을 한 줄로 세움. 신청할 수 없는 시설이면 null
    @Select("""
        SELECT f.id
          FROM facilities f
          JOIN public_facility_sources s ON s.facility_id = f.id AND s.claimed_at IS NULL
         WHERE f.id = #{facilityId} AND f.status = 'ACTIVE'
           FOR UPDATE
        """)
    Long lockClaimableFacility(@Param("facilityId") Long facilityId);

    @Select("SELECT COUNT(*) FROM facility_claims WHERE facility_id = #{facilityId} AND requester_user_id = #{requesterUserId} AND status = 'PENDING'")
    int countPending(@Param("facilityId") Long facilityId, @Param("requesterUserId") Long requesterUserId);

    @Select("SELECT COUNT(*) FROM facility_claims WHERE requester_user_id = #{requesterUserId} AND status = 'PENDING'")
    int countPendingByRequester(@Param("requesterUserId") Long requesterUserId);

    @Insert("""
        INSERT INTO facility_claims (facility_id, organization_id, requester_user_id, document_key, message, status)
        VALUES (#{facilityId}, #{organizationId}, #{requesterUserId}, #{documentKey}, #{message}, 'PENDING')
        """)
    int insert(@Param("facilityId") Long facilityId, @Param("organizationId") Long organizationId,
               @Param("requesterUserId") Long requesterUserId, @Param("documentKey") String documentKey,
               @Param("message") String message);

    @Select("""
        SELECT c.id, c.facility_id AS facilityId, f.name AS facilityName, f.address AS facilityAddress,
               f.facility_type AS facilityType, c.organization_id AS organizationId, o.name AS organizationName,
               c.message, c.status, c.reject_reason AS rejectReason,
               DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i') AS createdAt,
               DATE_FORMAT(c.decided_at, '%Y-%m-%d %H:%i') AS decidedAt
          FROM facility_claims c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = c.organization_id
         WHERE c.requester_user_id = #{requesterUserId}
         ORDER BY c.id DESC
        """)
    List<FacilityClaimResponse> findMine(@Param("requesterUserId") Long requesterUserId);

    @Select("""
        <script>
        SELECT c.id, c.facility_id AS facilityId, f.name AS facilityName, f.address AS facilityAddress,
               f.facility_type AS facilityType, c.organization_id AS organizationId, o.name AS organizationName,
               o.business_registration_number AS businessRegistrationNumber, v.status AS verificationStatus,
               u.name AS requesterName, u.phone AS requesterPhone, u.email AS requesterEmail,
               c.document_key AS documentKey, c.message, c.status, c.reject_reason AS rejectReason,
               DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i') AS createdAt,
               DATE_FORMAT(c.decided_at, '%Y-%m-%d %H:%i') AS decidedAt
          FROM facility_claims c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = c.organization_id
          JOIN users u ON u.id = c.requester_user_id
          LEFT JOIN business_verifications v ON v.organization_id = o.id
         <where>
           <if test="status != null">c.status = #{status}</if>
         </where>
         ORDER BY c.id DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<FacilityClaimResponse> findAdmin(@Param("status") String status, @Param("size") int size, @Param("offset") int offset);

    @Select("""
        <script>
        SELECT COUNT(*) FROM facility_claims c
         <where>
           <if test="status != null">c.status = #{status}</if>
         </where>
        </script>
        """)
    long countAdmin(@Param("status") String status);

    @Select("""
        SELECT id, facility_id AS facilityId, organization_id AS organizationId, status, document_key AS documentKey
          FROM facility_claims WHERE id = #{id} FOR UPDATE
        """)
    FacilityClaimResponse lockClaim(@Param("id") Long id);

    @Select("""
        SELECT id, document_key AS documentKey
          FROM facility_claims
         WHERE facility_id = #{facilityId} AND status = 'PENDING' AND id <> #{exceptId}
           FOR UPDATE
        """)
    List<FacilityClaimResponse> findOtherPending(@Param("facilityId") Long facilityId, @Param("exceptId") Long exceptId);

    @Select("SELECT document_key AS documentKey FROM facility_claims WHERE id = #{id}")
    FacilityClaimResponse findDocumentClaim(@Param("id") Long id);

    @Update("""
        UPDATE facility_claims
           SET status = 'APPROVED', decided_by = #{adminUserId}, decided_at = NOW(), document_key = NULL
         WHERE id = #{id}
        """)
    int markApproved(@Param("id") Long id, @Param("adminUserId") Long adminUserId);

    @Update("""
        UPDATE facility_claims
           SET status = 'REJECTED', reject_reason = #{reason}, decided_by = #{adminUserId}, decided_at = NOW(), document_key = NULL
         WHERE id = #{id}
        """)
    int markRejected(@Param("id") Long id, @Param("adminUserId") Long adminUserId, @Param("reason") String reason);

    @Update("UPDATE facilities SET organization_id = #{organizationId} WHERE id = #{facilityId}")
    int transferFacility(@Param("facilityId") Long facilityId, @Param("organizationId") Long organizationId);

    // 기록은 남겨 공공데이터 가져오기가 이 기관을 다시 넣거나 폐업으로 지우지 않게 함
    @Update("UPDATE public_facility_sources SET claimed_at = NOW() WHERE facility_id = #{facilityId}")
    int markSourceClaimed(@Param("facilityId") Long facilityId);

    // 소개 첫 줄의 "정보 출처: ..." 는 공공데이터 시설에만 맞는 문구
    @Update("""
        UPDATE facilities
           SET introduction = SUBSTRING(introduction, LOCATE(CHAR(10), introduction) + 1)
         WHERE id = #{facilityId} AND introduction LIKE '정보 출처:%' AND LOCATE(CHAR(10), introduction) > 0
        """)
    int stripPublicDataIntro(@Param("facilityId") Long facilityId);
}
