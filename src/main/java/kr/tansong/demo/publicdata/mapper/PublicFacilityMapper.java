package kr.tansong.demo.publicdata.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 공공데이터로 들여온 시설과 원본 기관코드의 연결 (public_facility_sources, resources/schema.sql) */
@Mapper
public interface PublicFacilityMapper {

    @Select("SELECT id FROM organizations WHERE name = #{name} ORDER BY id LIMIT 1")
    Long findOrganizationIdByName(@Param("name") String name);

    @Insert("""
        INSERT INTO organizations (name, organization_type, phone, email, business_registration_number, owner_user_id)
        VALUES (#{p.name}, #{p.type}, NULL, NULL, NULL, NULL)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "p.id")
    int insertOrganization(@Param("p") Map<String, Object> p);

    @Select("SELECT external_id FROM public_facility_sources WHERE source = #{source}")
    List<String> findExternalIds(@Param("source") String source);

    @Select("SELECT s.external_id FROM public_facility_sources s JOIN facilities f ON f.id = s.facility_id WHERE s.source = #{source} AND f.status = 'ACTIVE'")
    List<String> findLiveExternalIds(@Param("source") String source);

    @Delete("DELETE f FROM facilities f JOIN public_facility_sources s ON s.facility_id = f.id WHERE s.source = #{source} AND s.external_id = #{externalId}")
    int deleteFacility(@Param("source") String source, @Param("externalId") String externalId);

    @Update("UPDATE facilities f JOIN public_facility_sources s ON s.facility_id = f.id SET f.status = 'INACTIVE' WHERE s.source = #{source} AND s.external_id = #{externalId}")
    int deactivateFacility(@Param("source") String source, @Param("externalId") String externalId);

    // 공공데이터 시설은 관리자가 들여오는 것이라 바로 검색에 노출(ACTIVE)
    @Insert("""
        INSERT INTO facilities (organization_id, facility_type, name, address, phone, introduction, status)
        VALUES (#{f.organizationId}, #{f.facilityType}, #{f.name}, #{f.address}, #{f.phone}, #{f.introduction}, 'ACTIVE')
        """)
    @Options(useGeneratedKeys = true, keyProperty = "f.id")
    int insertFacility(@Param("f") Map<String, Object> f);

    @Insert("INSERT INTO public_facility_sources (facility_id, source, external_id) VALUES (#{facilityId}, #{source}, #{externalId})")
    int insertSource(@Param("facilityId") Long facilityId,
                     @Param("source") String source,
                     @Param("externalId") String externalId);
}
