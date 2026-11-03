package kr.tansong.demo.auth.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import kr.tansong.demo.common.AccountLockedException;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/** 비밀번호 확인과 실패 잠금. 로그인·비밀번호 변경·탈퇴가 실패 횟수를 함께 씀 (브루트포스 방지) */
@Component
public class PasswordVerifier {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final int maxLoginAttempts;
    private final int lockoutMinutes;

    public PasswordVerifier(UserMapper userMapper,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.auth.max-login-attempts:5}") int maxLoginAttempts,
                            @Value("${app.auth.lockout-minutes:15}") int lockoutMinutes) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    /** @throws AccountLockedException 잠긴 계정이면 비밀번호 확인 자체를 하지 않음 */
    public boolean verify(User user, String password) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil()));
            throw new AccountLockedException(
                    "비밀번호를 너무 많이 틀려 계정이 잠겼습니다. " + minutesLeft + "분 후 다시 시도해 주세요.");
        }
        // 입력이 없으면 추측한 것이 아니므로 세지 않음
        if (password == null) {
            return false;
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailure(user);
            return false;
        }
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            userMapper.resetLoginFailure(user.getId());
        }
        return true;
    }

    private void recordFailure(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;

        LocalDateTime lockedUntil = attempts >= maxLoginAttempts
                ? LocalDateTime.now().plusMinutes(lockoutMinutes)
                : null;

        // 잠금 걸리면 횟수는 0 으로 되돌려서, 잠금이 풀린 뒤엔 다시 maxLoginAttempts 번 시도할 수 있게 함
        userMapper.updateLoginFailure(user.getId(), lockedUntil != null ? 0 : attempts, lockedUntil);
    }
}
