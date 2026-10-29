package kr.tansong.demo.auth.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.auth.dto.AuthResponse;
import kr.tansong.demo.auth.dto.LoginRequest;
import kr.tansong.demo.auth.dto.SignupRequest;
import kr.tansong.demo.auth.dto.UserResponse;
import kr.tansong.demo.auth.security.JwtProvider;
import kr.tansong.demo.common.UnauthorizedException;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

@Service
public class AuthService {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9\\-]{9,15}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final int maxLoginAttempts;
    private final int lockoutMinutes;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       @Value("${app.auth.max-login-attempts:5}") int maxLoginAttempts,
                       @Value("${app.auth.lockout-minutes:15}") int lockoutMinutes) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("가입 정보가 없습니다.");
        }

        String name = trim(req.getName());
        String email = normalizeEmail(req.getEmail());
        String phone = trim(req.getPhone());
        String password = req.getPassword();

        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("이름을 1~100자로 입력해 주세요.");
        }
        if (email.length() > 150 || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("전화번호는 숫자와 하이픈(-)만 입력해 주세요.");
        }
        if (password == null || password.length() < 8 || password.length() > 64
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 포함해 8~64자로 입력해 주세요.");
        }

        if (userMapper.findByEmail(email) != null) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 동시 가입으로 위 확인을 통과한 경우
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        if (req == null || req.getPassword() == null) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userMapper.findByEmail(normalizeEmail(req.getEmail()));

        // 이메일 자체가 없는 경우는 실패 횟수를 기록할 대상이 없으니 바로 공통 메시지로 거절
        if (user == null) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 잠긴 계정은 비밀번호 확인 자체를 하지 않음 (브루트포스 방지)
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil()));
            throw new UnauthorizedException(
                    "로그인 시도가 너무 많아 계정이 잠겼습니다. " + minutesLeft + "분 후 다시 시도해 주세요.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            recordFailedLogin(user);
            // 이메일/비밀번호 중 무엇이 틀렸는지 알려주지 않음
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            userMapper.resetLoginFailure(user.getId());
        }

        return toAuthResponse(user);
    }

    private void recordFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;

        LocalDateTime lockedUntil = attempts >= maxLoginAttempts
                ? LocalDateTime.now().plusMinutes(lockoutMinutes)
                : null;

        // 잠금 걸리면 횟수는 0 으로 되돌려서, 잠금이 풀린 뒤엔 다시 maxLoginAttempts 번의 기회를 줌
        userMapper.updateLoginFailure(user.getId(), lockedUntil != null ? 0 : attempts, lockedUntil);
    }

    public UserResponse me(Long userId) {
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new UnauthorizedException("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
        }

        return new UserResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtProvider.createToken(user.getId(), user.getRole());
        return new AuthResponse(token, new UserResponse(user));
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String normalizeEmail(String s) {
        return trim(s).toLowerCase();
    }
}
