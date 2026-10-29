package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import kr.tansong.demo.facility.dto.FacilityImageResponse;

/** 시설 사진(facility_images) 조회/등록/삭제 */
@Mapper
public interface FacilityImageMapper {

    @Select("""
        SELECT image_url
          FROM facility_images
         WHERE facility_id = #{facilityId}
         ORDER BY sort_order, id
        """)
    List<String> findImageUrlsByFacilityId(@Param("facilityId") Long facilityId);

    @Select("""
        SELECT id, facility_id AS facilityId, image_url AS imageUrl, sort_order AS sortOrder
          FROM facility_images
         WHERE facility_id = #{facilityId}
         ORDER BY sort_order, id
        """)
    List<FacilityImageResponse> findByFacilityId(@Param("facilityId") Long facilityId);

    @Select("SELECT image_url FROM facility_images WHERE id = #{id} AND facility_id = #{facilityId}")
    String findImageUrl(@Param("id") Long id, @Param("facilityId") Long facilityId);

    @Select("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM facility_images WHERE facility_id = #{facilityId}")
    int nextSortOrder(@Param("facilityId") Long facilityId);

    @Insert("""
        INSERT INTO facility_images (facility_id, image_url, sort_order)
        VALUES (#{facilityId}, #{imageUrl}, #{sortOrder})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FacilityImageResponse image);

    @Delete("DELETE FROM facility_images WHERE id = #{id} AND facility_id = #{facilityId}")
    int deleteByIdAndFacilityId(@Param("id") Long id, @Param("facilityId") Long facilityId);
}
