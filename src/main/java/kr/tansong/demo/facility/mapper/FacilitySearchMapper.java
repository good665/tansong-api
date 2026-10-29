package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import kr.tansong.demo.facility.dto.FacilityResponse;

/** 기존 FacilityMapper(XML)는 건드리지 않고, 페이징 검색만 어노테이션으로 분리 */
@Mapper
public interface FacilitySearchMapper {

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
               f.admission_conditions  AS admissionConditions
          FROM facilities f
          JOIN organizations o ON o.id = f.organization_id
        <where>
            f.status = 'ACTIVE'
            <if test="type != null and type != ''">
                AND f.facility_type = #{type}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (f.name    LIKE CONCAT('%', #{keyword}, '%')
                  OR f.address LIKE CONCAT('%', #{keyword}, '%')
                  OR o.name    LIKE CONCAT('%', #{keyword}, '%'))
            </if>
        </where>
         ORDER BY f.id DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<FacilityResponse> search(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset);

    @Select("""
        <script>
        SELECT COUNT(*)
          FROM facilities f
          JOIN organizations o ON o.id = f.organization_id
        <where>
            f.status = 'ACTIVE'
            <if test="type != null and type != ''">
                AND f.facility_type = #{type}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (f.name    LIKE CONCAT('%', #{keyword}, '%')
                  OR f.address LIKE CONCAT('%', #{keyword}, '%')
                  OR o.name    LIKE CONCAT('%', #{keyword}, '%'))
            </if>
        </where>
        </script>
        """)
    long count(
            @Param("type") String type,
            @Param("keyword") String keyword);
}
