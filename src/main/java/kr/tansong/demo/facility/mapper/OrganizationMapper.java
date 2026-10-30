package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import kr.tansong.demo.admin.dto.OrganizationCreateRequest;
import kr.tansong.demo.admin.dto.OrganizationSummaryResponse;

/** 시설 등록/수정 화면에서 운영 기관을 고르고, 새 기관을 등록할 수 있도록 지원 (관리자 전용 + 사업자 셀프 가입) */
@Mapper
public interface OrganizationMapper {

    @Select("""
        SELECT id, name, organization_type AS organizationType
          FROM organizations
         WHERE status = 'ACTIVE'
         ORDER BY name
        """)
    List<OrganizationSummaryResponse> findAllActive();

    @Insert("""
        INSERT INTO organizations (
            name, organization_type, phone, email,
            business_registration_number, owner_user_id
        ) VALUES (
            #{name}, #{organizationType}, #{phone}, #{email},
            #{businessRegistrationNumber}, #{ownerUserId}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrganizationCreateRequest request);

    // 이 조직이 해당 사용자 소유인지 (사업자가 시설을 등록할 organizationId 가 본인 것인지 확인용)
    @Select("SELECT COUNT(*) FROM organizations WHERE id = #{organizationId} AND owner_user_id = #{ownerUserId}")
    int countOwnedByUser(@Param("organizationId") Long organizationId, @Param("ownerUserId") Long ownerUserId);

    @Select("""
        SELECT id, name, organization_type AS organizationType
          FROM organizations
         WHERE owner_user_id = #{ownerUserId}
         ORDER BY name
        """)
    List<OrganizationSummaryResponse> findAllOwnedByUser(@Param("ownerUserId") Long ownerUserId);
}
