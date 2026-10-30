package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.tansong.demo.admin.dto.FacilityUpsertRequest;
import kr.tansong.demo.facility.dto.FacilityResponse;

/** 사업자(시설 운영자) 본인 소유 시설만 다루는 CRUD. 모든 쿼리가 organizations.owner_user_id 로
 *  소유권을 직접 확인하므로, 다른 사업자의 시설 id 를 넘겨도 조회/수정되지 않습니다. */
@Mapper
public interface BusinessFacilityMapper {

    @Select("""
        <script>
        SELECT f.id,
               f.organization_id       AS organizationId,
               o.name                  AS organizationName,
               f.facility_type         AS facilityType,
               f.name,
               f.address,
               f.phone,
               f.monthly_fee_min       AS monthlyFeeMin,
               f.monthly_fee_max       AS monthlyFeeMax,
               f.introduction,
               f.admission_conditions  AS admissionConditions,
               f.status
          FROM facilities f
          JOIN organizations o ON o.id = f.organization_id
         WHERE o.owner_user_id = #{ownerUserId}
        <if test="status != null and status != ''">
            AND f.status = #{status}
        </if>
         ORDER BY f.id DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<FacilityResponse> findAllByOwner(
            @Param("ownerUserId") Long ownerUserId,
            @Param("status") String status,
            @Param("size") int size,
            @Param("offset") int offset);

    @Select("""
        <script>
        SELECT COUNT(*)
          FROM facilities f
          JOIN organizations o ON o.id = f.organization_id
         WHERE o.owner_user_id = #{ownerUserId}
        <if test="status != null and status != ''">
            AND f.status = #{status}
        </if>
        </script>
        """)
    long countByOwner(@Param("ownerUserId") Long ownerUserId, @Param("status") String status);

    @Select("""
        SELECT f.id,
               f.organization_id       AS organizationId,
               o.name                  AS organizationName,
               f.facility_type         AS facilityType,
               f.name,
               f.address,
               f.phone,
               f.monthly_fee_min       AS monthlyFeeMin,
               f.monthly_fee_max       AS monthlyFeeMax,
               f.introduction,
               f.admission_conditions  AS admissionConditions,
               f.status
          FROM facilities f
          JOIN organizations o ON o.id = f.organization_id
         WHERE f.id = #{id} AND o.owner_user_id = #{ownerUserId}
        """)
    FacilityResponse findByIdForOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    // 사업자가 등록한 시설은 항상 PENDING 으로 시작 — 관리자 승인 전까지 공개 목록(status='ACTIVE' 전용)에 노출되지 않음
    @Insert("""
        INSERT INTO facilities (
            organization_id, facility_type, name, address, phone,
            monthly_fee_min, monthly_fee_max, introduction, admission_conditions, status
        ) VALUES (
            #{organizationId}, #{facilityType}, #{name}, #{address}, #{phone},
            #{monthlyFeeMin}, #{monthlyFeeMax}, #{introduction}, #{admissionConditions}, 'PENDING'
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FacilityUpsertRequest request);

    @Update("""
        UPDATE facilities
           SET organization_id = #{request.organizationId},
               facility_type = #{request.facilityType},
               name = #{request.name},
               address = #{request.address},
               phone = #{request.phone},
               monthly_fee_min = #{request.monthlyFeeMin},
               monthly_fee_max = #{request.monthlyFeeMax},
               introduction = #{request.introduction},
               admission_conditions = #{request.admissionConditions}
         WHERE id = #{id}
           AND organization_id IN (SELECT id FROM organizations WHERE owner_user_id = #{ownerUserId})
        """)
    int updateForOwner(@Param("id") Long id,
                        @Param("ownerUserId") Long ownerUserId,
                        @Param("request") FacilityUpsertRequest request);
}
