package kr.tansong.demo.auth.dto;

import kr.tansong.demo.user.domain.User;

public class UserResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String role;

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
}
