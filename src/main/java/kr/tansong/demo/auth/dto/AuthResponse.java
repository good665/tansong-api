package kr.tansong.demo.auth.dto;

public class AuthResponse {

    private final String token;
    private final UserResponse user;

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public UserResponse getUser() { return user; }
}
