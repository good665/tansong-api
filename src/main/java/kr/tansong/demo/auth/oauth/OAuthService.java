package kr.tansong.demo.auth.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.auth.dto.AuthResponse;
import kr.tansong.demo.auth.oauth.mapper.OAuthMapper;
import kr.tansong.demo.auth.service.AuthService;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/**
 * 소셜 로그인 흐름: 앱 → start(제공자로 이동) → 제공자 → finish(사용자 확인) → 앱으로 일회용 코드 → exchange(JWT 발급).
 * JWT 는 주소에 넣지 않습니다. 복귀 주소는 허용 목록과 정확히 같아야 합니다.
 * PKCE: 시작한 기기만 아는 verifier 가 있어야 코드를 교환할 수 있어, 코드를 가로채거나 남의 로그인 링크를 열어도 소용없습니다.
 */
@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private static final String APP_REDIRECT = "tansongmobile://oauth-callback";
    private static final int STATE_SECONDS = 600;
    private static final int LOGIN_CODE_SECONDS = 60;
    private static final Pattern CODE_CHALLENGE = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Map<String, String> PROVIDER_NAMES = Map.of("google", "구글", "naver", "네이버", "kakao", "카카오");

    private final SecureRandom random = new SecureRandom();
    private final OAuthClient client;
    private final OAuthMapper mapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;
    private final Set<String> allowedRedirects = new HashSet<>();

    public OAuthService(OAuthClient client, OAuthMapper mapper, UserMapper userMapper, AuthService authService,
                        PasswordEncoder passwordEncoder, TransactionTemplate tx,
                        @Value("${app.oauth.web-redirects:}") String webRedirects) {
        this.client = client;
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.tx = tx;
        allowedRedirects.add(APP_REDIRECT);
        Arrays.stream(webRedirects.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(allowedRedirects::add);
    }

    public List<String> providers() {
        return client.enabled();
    }

    public String startUrl(String provider, String redirect, String codeChallenge) {
        if (!client.isEnabled(provider)) {
            throw new IllegalArgumentException("사용할 수 없는 로그인 방식입니다.");
        }
        if (!allowedRedirects.contains(redirect)) {
            throw new IllegalArgumentException("허용되지 않은 복귀 주소입니다.");
        }
        if (codeChallenge == null || !CODE_CHALLENGE.matcher(codeChallenge).matches()) {
            throw new IllegalArgumentException("로그인 요청이 올바르지 않습니다.");
        }
        mapper.deleteExpired();
        String state = newToken();
        mapper.insertTicket(hash(state), "STATE", provider, redirect, codeChallenge, null, STATE_SECONDS);
        return client.authorizeUrl(provider, state, client.callbackUrl(provider));
    }

    /** 제공자가 돌아온 뒤 앱으로 보낼 주소를 돌려줌. 상태값이 틀리면 믿을 복귀 주소가 없으므로 예외 */
    public String finish(String provider, String code, String state, String error) {
        OAuthMapper.Ticket ticket = consume(state, "STATE");
        if (ticket == null || !provider.equals(ticket.provider)) {
            throw new IllegalArgumentException("로그인 요청이 만료되었습니다. 다시 시도해 주세요.");
        }
        String redirect = ticket.redirectUri;

        if (error != null || code == null || code.isBlank()) {
            return redirect + "?error=cancelled";
        }

        try {
            OAuthClient.Profile profile = client.fetchProfile(provider, code, state, client.callbackUrl(provider));
            Long userId = findOrCreateUser(provider, profile);
            if (userId == null) {
                return redirect + "?error=email_exists";
            }
            String loginCode = newToken();
            mapper.insertTicket(hash(loginCode), "LOGIN", provider, redirect, ticket.codeChallenge, userId, LOGIN_CODE_SECONDS);
            return redirect + "?code=" + loginCode;
        } catch (RuntimeException e) {
            // 메시지에 토큰이나 코드가 섞일 수 있어 예외 종류만 기록
            log.warn("소셜 로그인 실패: provider={}, type={}", provider, e.getClass().getSimpleName());
            return redirect + "?error=failed";
        }
    }

    public AuthResponse exchange(String code, String codeVerifier) {
        String hash = code == null || code.isBlank() ? null : hash(code);
        OAuthMapper.Ticket ticket = hash == null ? null : mapper.findTicket(hash, "LOGIN");
        // verifier 확인 전에는 코드를 지우지 않음: 코드만 가로챈 쪽이 정상 로그인을 방해하지 못하게
        if (ticket == null || ticket.userId == null || !matchesChallenge(codeVerifier, ticket.codeChallenge)
                || mapper.deleteTicket(hash) != 1) {
            throw new IllegalArgumentException("로그인 코드가 만료되었습니다. 다시 시도해 주세요.");
        }
        return authService.issueToken(ticket.userId);
    }

    private static boolean matchesChallenge(String verifier, String challenge) {
        if (verifier == null || challenge == null) {
            return false;
        }
        String actual = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII), challenge.getBytes(StandardCharsets.US_ASCII));
    }

    /** 사용자 id. 같은 이메일의 다른 계정이 있으면 null */
    private Long findOrCreateUser(String provider, OAuthClient.Profile profile) {
        Long linked = mapper.findLinkedUser(provider, profile.providerUserId());
        if (linked != null) {
            return linked;
        }

        boolean useEmail = profile.email() != null && profile.emailVerified();
        String email = useEmail
                ? profile.email().trim().toLowerCase()
                // shortcut: 이메일이 없거나 인증되지 않으면 임시 주소를 저장(users.email 이 NOT NULL). 내 정보 수정 기능을 만들 때 실제 이메일을 입력받도록 변경
                : provider + "_" + profile.providerUserId() + "@social.invalid";

        // 이메일 가입은 인증이 없어, 자동 연결하면 미리 만들어 둔 계정으로 탈취될 수 있음
        if (useEmail && userMapper.findByEmail(email) != null) {
            return null;
        }

        try {
            return tx.execute(status -> {
                User user = new User();
                user.setName(displayName(provider, profile.name()));
                user.setEmail(email);
                user.setRole("USER");
                // 비밀번호 로그인은 쓰지 않으므로 아무도 모르는 값
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                userMapper.insert(user);
                mapper.insertLink(provider, profile.providerUserId(), user.getId());
                return user.getId();
            });
        } catch (DuplicateKeyException e) {
            // 동시 요청이 먼저 만들었거나, 같은 이메일이 먼저 가입됨
            return mapper.findLinkedUser(provider, profile.providerUserId());
        }
    }

    private static String displayName(String provider, String name) {
        if (name == null || name.isBlank()) {
            return PROVIDER_NAMES.getOrDefault(provider, "소셜") + " 사용자";
        }
        String trimmed = name.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private OAuthMapper.Ticket consume(String token, String kind) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String hash = hash(token);
        OAuthMapper.Ticket ticket = mapper.findTicket(hash, kind);
        return ticket != null && mapper.deleteTicket(hash) == 1 ? ticket : null;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        return HexFormat.of().formatHex(sha256(token));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
