package kr.tansong.demo.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.auth.dto.AuthResponse;
import kr.tansong.demo.auth.dto.BusinessSignupRequest;
import kr.tansong.demo.auth.dto.LoginRequest;
import kr.tansong.demo.auth.dto.SignupRequest;
import kr.tansong.demo.auth.dto.UserResponse;
import kr.tansong.demo.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 가입 즉시 로그인된 상태로 토큰을 돌려줌
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 사업자(시설 운영자) 가입: 개인 계정 + 소유 기관을 한 번에 생성, role=BUSINESS_OWNER
    @PostMapping("/signup-business")
    public ResponseEntity<AuthResponse> signupBusiness(@RequestBody BusinessSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupBusiness(request));
    }

    // 저장된 토큰이 아직 유효한지 + 내 정보 조회
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Long userId) {
        return authService.me(userId);
    }
}
