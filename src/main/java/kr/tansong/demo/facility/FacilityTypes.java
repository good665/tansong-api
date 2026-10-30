package kr.tansong.demo.facility;

import java.util.Set;

/** 시설 종류 enum 값 (관리자/사업자 양쪽의 시설 등록 검증에서 공용으로 사용) */
public final class FacilityTypes {

    public static final Set<String> VALID = Set.of(
            "SILVER_TOWN", "NURSING_HOME", "NURSING_HOSPITAL", "DAY_CARE", "HOME_CARE");

    private FacilityTypes() {
    }
}
