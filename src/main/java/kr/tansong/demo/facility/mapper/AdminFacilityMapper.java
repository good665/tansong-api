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

/** 관리자용 시설 CRUD. 공개 조회(FacilityMapper/FacilitySearchMapper)는 status='ACTIVE'만 보지만
 *  관리자는 상태와 무관하게 전부 다룰 수 있어야 하므로 별도 매퍼로 분리합니다. */
@Mapper
public interface AdminFacilityMapper {

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
        <where>
            <if test="status != null and status != ''">
                AND f.status = #{status}
            </if>
        </where>
         ORDER BY f.id DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<FacilityResponse> findAllAdmin(
            @Param("status") String status,
            @Param("size") int size,
            @Param("offset") int offset);

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
         WHERE f.id = #{id}
        """)
    FacilityResponse findByIdAdmin(@Param("id") Long id);

    @Select("""
        <script>
        SELECT COUNT(*)
          FROM facilities f
        <where>
            <if test="status != null and status != ''">
                AND f.status = #{status}
            </if>
        </where>
        </script>
        """)
    long countAdmin(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM organizations WHERE id = #{organizationId}")
    int countOrganizationById(@Param("organizationId") Long organizationId);

    @Insert("""
        INSERT INTO facilities (
            organization_id, facility_type, name, address, phone,
            monthly_fee_min, monthly_fee_max, introduction, admission_conditions
        ) VALUES (
            #{organizationId}, #{facilityType}, #{name}, #{address}, #{phone},
            #{monthlyFeeMin}, #{monthlyFeeMax}, #{introduction}, #{admissionConditions}
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
        """)
    int update(@Param("id") Long id, @Param("request") FacilityUpsertRequest request);

    @Update("UPDATE facilities SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
