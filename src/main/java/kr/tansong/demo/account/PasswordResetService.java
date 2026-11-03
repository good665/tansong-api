package kr.tansong.demo.account;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.Executor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.account.mapper.AccountMapper;
import kr.tansong.demo.auth.service.AuthService;
import kr.tansong.demo.common.ServiceUnavailableException;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/**
 * 이메일 6자리 코드로 비밀번호 재설정. 가입 여부가 드러나지 않도록 요청은 항상 같은 응답을 주고,
 * 메일은 응답 시간에 영향이 없도록 별도 스레드에서 보냅니다.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int CODE_SECONDS = 600;
    private static final int RESEND_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final String INVALID = "코드가 올바르지 않거나 만료되었습니다. 코드를 다시 받아 주세요.";

    private final SecureRandom random = new SecureRandom();
    private final AccountMapper mapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final TransactionTemplate tx;
    private final Executor executor;
    private final byte[] hashKey;

    public PasswordResetService(AccountMapper mapper, UserMapper userMapper, PasswordEncoder passwordEncoder,
                                PasswordResetMailer mailer, TransactionTemplate tx,
                                @Qualifier("applicationTaskExecutor") Executor executor,
                                @Value("${app.jwt.secret}") String secret) {
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.tx = tx;
        this.executor = executor;
        this.hashKey = ("password-reset:" + secret).getBytes(StandardCharsets.UTF_8);
    }

    public void request(String email) {
        if (!mailer.isConfigured()) {
            throw new ServiceUnavailableException("지금은 비밀번호 찾기를 사용할 수 없습니다. 고객센터로 문의해 주세요.");
        }
        User user = findUser(email);
        // 소셜 계정은 비밀번호가 없음
        if (user == null || mapper.hasSocialAccount(user.getId()) || mapper.hasRecentResetCode(user.getId(), RESEND_SECONDS)) {
            return;
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        mapper.saveResetCode(user.getId(), hash(user.getId(), code), CODE_SECONDS);

        String to = user.getEmail();
        executor.execute(() -> {
            try {
                mailer.send(to, code);
            } catch (RuntimeException e) {
                // 코드나 주소를 남기지 않음
                log.warn("비밀번호 재설정 메일 발송 실패: {}", e.getClass().getSimpleName());
            }
        });
    }

    public void confirm(String email, String code, String newPassword) {
        AuthService.validatePassword(newPassword);

        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException(INVALID);
        }
        User user = findUser(email);
        AccountMapper.ResetCode stored = user == null ? null : mapper.findValidResetCode(user.getId());
        if (stored == null || stored.attempts >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException(INVALID);
        }
        // 맞는지 확인하기 전에 먼저 올려 동시 요청으로 횟수 제한을 넘지 못하게 함
        mapper.incrementResetAttempts(user.getId());

        boolean matches = MessageDigest.isEqual(
                hash(user.getId(), code.trim()).getBytes(StandardCharsets.US_ASCII),
                stored.codeHash.getBytes(StandardCharsets.US_ASCII));
        if (!matches) {
            throw new IllegalArgumentException(INVALID);
        }

        String encoded = passwordEncoder.encode(newPassword);
        tx.executeWithoutResult(status -> {
            userMapper.updatePassword(user.getId(), encoded);
            userMapper.resetLoginFailure(user.getId());
            mapper.markPasswordChanged(user.getId());
            mapper.deletePasswordResetCode(user.getId());
        });
    }

    private User findUser(String email) {
        User user = email == null ? null : userMapper.findByEmail(email.trim().toLowerCase());
        if (user == null) return null;
        AccountMapper.AccountStatus status = mapper.findAccountStatus(user.getId());
        return status != null && status.deleted ? null : user;
    }

    private String hash(Long userId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
