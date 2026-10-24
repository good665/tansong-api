
package kr.tansong.demo.facility.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.tansong.demo.facility.dto.FacilityResponse;

@Mapper
public interface FacilityMapper {

    List<FacilityResponse> findAll(
        @Param("type") String type,
        @Param("keyword") String keyword
    );
    
    // 추가: 시설 ID로 상세 조회
    FacilityResponse findById(@Param("id") Long id);
}
