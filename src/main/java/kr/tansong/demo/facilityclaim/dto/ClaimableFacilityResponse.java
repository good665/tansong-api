package kr.tansong.demo.facilityclaim.dto;

/** 소유권을 신청할 수 있는 공공데이터 시설 */
public class ClaimableFacilityResponse {

    public Long id;
    public String name;
    public String address;
    public String phone;
    public String facilityType;
    public boolean myPending; // 내가 이미 신청해 대기 중인지
}
