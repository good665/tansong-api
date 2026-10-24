package kr.tansong.demo.facility.dto;

import java.math.BigDecimal;

public class FacilityResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private String facilityType;
    private String name;
    private String address;
    private String phone;
    private BigDecimal monthlyFeeMin;
    private BigDecimal monthlyFeeMax;
    private String introduction;
    private String admissionConditions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getMonthlyFeeMin() {
        return monthlyFeeMin;
    }

    public void setMonthlyFeeMin(BigDecimal monthlyFeeMin) {
        this.monthlyFeeMin = monthlyFeeMin;
    }

    public BigDecimal getMonthlyFeeMax() {
        return monthlyFeeMax;
    }

    public void setMonthlyFeeMax(BigDecimal monthlyFeeMax) {
        this.monthlyFeeMax = monthlyFeeMax;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getAdmissionConditions() {
        return admissionConditions;
    }

    public void setAdmissionConditions(String admissionConditions) {
        this.admissionConditions = admissionConditions;
    }
}
