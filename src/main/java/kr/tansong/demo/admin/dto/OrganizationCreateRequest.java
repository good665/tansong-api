package kr.tansong.demo.admin.dto;

public class OrganizationCreateRequest {

    // 등록 시 MyBatis 가 생성된 PK 를 채워 넣는 용도 (요청 시점에는 비어 있음)
    private Long id;
    private String name;
    private String organizationType;
    private String phone;
    private String email;
    private String businessRegistrationNumber;
    // 사업자 셀프 가입(AuthService#signupBusiness) 때만 채워짐. 관리자가 등록할 땐 null(소유자 없음).
    private Long ownerUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public void setBusinessRegistrationNumber(String businessRegistrationNumber) {
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
