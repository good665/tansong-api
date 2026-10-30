package kr.tansong.demo.auth.dto;

public class BusinessSignupRequest {

    // 가입자(사업자 담당자) 개인 정보
    private String name;
    private String email;
    private String phone;
    private String password;

    // 운영 기관(사업체) 정보 — 가입과 동시에 기관이 생성되고 이 계정이 소유자가 됨
    private String organizationName;
    private String organizationType;
    private String businessRegistrationNumber;
    private String organizationPhone;
    private String organizationEmail;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getOrganizationType() { return organizationType; }
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }
    public String getBusinessRegistrationNumber() { return businessRegistrationNumber; }
    public void setBusinessRegistrationNumber(String businessRegistrationNumber) { this.businessRegistrationNumber = businessRegistrationNumber; }
    public String getOrganizationPhone() { return organizationPhone; }
    public void setOrganizationPhone(String organizationPhone) { this.organizationPhone = organizationPhone; }
    public String getOrganizationEmail() { return organizationEmail; }
    public void setOrganizationEmail(String organizationEmail) { this.organizationEmail = organizationEmail; }
}
