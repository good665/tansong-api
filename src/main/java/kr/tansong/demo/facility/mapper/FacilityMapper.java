
package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import kr.tansong.demo.facility.dto.FacilityResponse;

@Mapper
public interface FacilityMapper {

    List<FacilityResponse> findAll(
        @Param("type") String type,
        @Param("keyword") String keyword
    );

    // 추가: 시설 ID로 상세 조회
    FacilityResponse findById(@Param("id") Long id);

    // status 와 무관하게 존재 여부만 확인 (관리자용: 비활성 시설도 관리 대상이므로)
    @Select("SELECT COUNT(*) FROM facilities WHERE id = #{id}")
    int countById(@Param("id") Long id);
}
