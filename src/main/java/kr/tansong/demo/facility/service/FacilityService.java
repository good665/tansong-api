
package kr.tansong.demo.facility.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.FacilityMapper;

@Service
public class FacilityService {

    private final FacilityMapper facilityMapper;

    public FacilityService(FacilityMapper facilityMapper) {
        this.facilityMapper = facilityMapper;
    }
    // 시설 목록 조회 및 검색
    public List<FacilityResponse> findAll(
            String type,
            String keyword) {

        return facilityMapper.findAll(type, keyword);
    }
 // 시설 상세 조회
    public FacilityResponse getFacilityById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("시설 ID가 올바르지 않습니다.");
        }

        return facilityMapper.findById(id);
    }
}
