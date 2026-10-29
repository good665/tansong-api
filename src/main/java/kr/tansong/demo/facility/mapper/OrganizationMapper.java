package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import kr.tansong.demo.admin.dto.OrganizationCreateRequest;
import kr.tansong.demo.admin.dto.OrganizationSummaryResponse;

/** 시설 등록/수정 화면에서 운영 기관을 고르고, 새 기관을 등록할 수 있도록 지원 (관리자 전용) */
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
        INSERT INTO organizations (name, organization_type, phone, email)
        VALUES (#{name}, #{organizationType}, #{phone}, #{email})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrganizationCreateRequest request);
}
