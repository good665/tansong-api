package kr.tansong.demo.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.auth.dto.AuthResponse;
import kr.tansong.demo.auth.oauth.mapper.OAuthMapper;
import kr.tansong.demo.auth.service.AuthService;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuthServiceTest {

    private static final String APP = "tansongmobile://oauth-callback";
    // RFC 7636 부록 B 의 예시 값
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String WEB = "http://localhost:8081/oauth-callback";

    @Mock
    private OAuthClient client;
    @Mock
    private OAuthMapper mapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthService authService;
    @Mock
    private PasswordEncoder encoder;

    private OAuthService service;

    @BeforeEach
    void setUp() {
        service = new OAuthService(client, mapper, userMapper, authService, encoder,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                WEB + ", http://127.0.0.1:8081/oauth-callback");
        when(client.isEnabled("kakao")).thenReturn(true);
        when(client.callbackUrl("kakao")).thenReturn("http://server/cb");
        when(client.authorizeUrl(eq("kakao"), anyString(), anyString())).thenReturn("https://kakao/authorize");
        when(encoder.encode(anyString())).thenReturn("hash");
        when(mapper.findLinkedUser(anyString(), anyString())).thenReturn(null); // Long 기본 응답은 0L 이라 명시
        doAnswer(i -> {
            i.<User>getArgument(0).setId(50L);
            return 1;
        }).when(userMapper).insert(any());
    }

    private void validState() {
        OAuthMapper.Ticket ticket = new OAuthMapper.Ticket();
        ticket.provider = "kakao";
        ticket.redirectUri = APP;
        ticket.codeChallenge = CHALLENGE;
        when(mapper.findTicket(anyString(), eq("STATE"))).thenReturn(ticket);
        when(mapper.deleteTicket(anyString())).thenReturn(1);
    }

    private void profile(String id, String name, String email, boolean verified) {
        when(client.fetchProfile(eq("kakao"), eq("c"), eq("s"), anyString()))
                .thenReturn(new OAuthClient.Profile(id, name, email, verified));
    }

    @Test
    void 시작하면_상태값은_해시로만_저장하고_제공자로_보낸다() {
        assertThat(service.startUrl("kakao", APP, CHALLENGE)).isEqualTo("https://kakao/authorize");

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertTicket(hash.capture(), eq("STATE"), eq("kakao"), eq(APP), eq(CHALLENGE), eq(null), anyInt());
        verify(client).authorizeUrl(eq("kakao"), state.capture(), eq("http://server/cb"));
        assertThat(state.getValue()).hasSizeGreaterThan(30).isNotEqualTo(hash.getValue());
        assertThat(hash.getValue()).hasSize(64);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example/cb", "tansongmobile://oauth-callback.evil", "tansongmobile://oauth-callback?x=1",
            "http://localhost:8081/oauth-callback/../x", "javascript:alert(1)", ""})
    void 허용되지_않은_복귀_주소는_거절한다(String redirect) {
        assertThatThrownBy(() -> service.startUrl("kakao", redirect, CHALLENGE)).isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).insertTicket(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void 설정된_웹_주소는_허용하고_설정_안된_제공자는_거절한다() {
        assertThat(service.startUrl("kakao", "http://127.0.0.1:8081/oauth-callback", CHALLENGE)).isNotNull();
        assertThatThrownBy(() -> service.startUrl("google", APP, CHALLENGE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상태값이_틀리면_어디로도_보내지_않고_거절한다() {
        when(mapper.findTicket(anyString(), eq("STATE"))).thenReturn(null);

        assertThatThrownBy(() -> service.finish("kakao", "c", "s", null)).isInstanceOf(IllegalArgumentException.class);
        verify(client, never()).fetchProfile(any(), any(), any(), any());
    }

    @Test
    void 상태값은_한번만_쓸_수_있다() {
        validState();
        when(mapper.deleteTicket(anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.finish("kakao", "c", "s", null)).isInstanceOf(IllegalArgumentException.class);
        verify(client, never()).fetchProfile(any(), any(), any(), any());
    }

    @Test
    void 다른_제공자로_시작한_상태값은_거절한다() {
        validState();

        assertThatThrownBy(() -> service.finish("google", "c", "s", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 사용자가_거부하면_앱으로_취소를_알린다() {
        validState();

        assertThat(service.finish("kakao", null, "s", "access_denied")).isEqualTo(APP + "?error=cancelled");
        verify(client, never()).fetchProfile(any(), any(), any(), any());
    }

    @Test
    void 연결된_계정은_새로_만들지_않고_일회용_코드를_보낸다() {
        validState();
        profile("9", "홍길동", "a@b.com", true);
        when(mapper.findLinkedUser("kakao", "9")).thenReturn(7L);

        String url = service.finish("kakao", "c", "s", null);

        assertThat(url).startsWith(APP + "?code=").doesNotContain("eyJ");
        verify(userMapper, never()).insert(any());
        verify(mapper).insertTicket(anyString(), eq("LOGIN"), eq("kakao"), eq(APP), eq(CHALLENGE), eq(7L), anyInt());
    }

    @Test
    void 처음이면_USER_계정과_연결을_만든다() {
        validState();
        profile("9", "홍길동", "NEW@b.com", true);

        service.finish("kakao", "c", "s", null);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(user.capture());
        assertThat(user.getValue().getRole()).isEqualTo("USER");
        assertThat(user.getValue().getEmail()).isEqualTo("new@b.com");
        assertThat(user.getValue().getName()).isEqualTo("홍길동");
        verify(mapper).insertLink("kakao", "9", 50L);
    }

    // 이메일 가입은 인증을 하지 않아, 자동 연결하면 미리 만든 계정으로 탈취될 수 있음
    @Test
    void 같은_이메일의_기존_계정과_자동_연결하지_않는다() {
        validState();
        profile("9", "홍길동", "a@b.com", true);
        when(userMapper.findByEmail("a@b.com")).thenReturn(new User());

        assertThat(service.finish("kakao", "c", "s", null)).isEqualTo(APP + "?error=email_exists");
        verify(mapper, never()).insertLink(any(), any(), any());
        verify(mapper, never()).insertTicket(anyString(), eq("LOGIN"), any(), any(), any(), any(), anyInt());
    }

    @Test
    void 인증되지_않았거나_없는_이메일은_임시_주소로_만든다() {
        validState();
        profile("9", null, "a@b.com", false);
        when(userMapper.findByEmail("a@b.com")).thenReturn(new User());

        service.finish("kakao", "c", "s", null);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("kakao_9@social.invalid");
        assertThat(user.getValue().getName()).isEqualTo("카카오 사용자");
    }

    @Test
    void 동시에_가입되면_먼저_만들어진_연결로_로그인한다() {
        validState();
        profile("9", "홍길동", "a@b.com", true);
        when(mapper.findLinkedUser("kakao", "9")).thenReturn(null, 7L);
        doThrow(new DuplicateKeyException("dup")).when(userMapper).insert(any());

        assertThat(service.finish("kakao", "c", "s", null)).startsWith(APP + "?code=");
        verify(mapper).insertTicket(anyString(), eq("LOGIN"), eq("kakao"), eq(APP), eq(CHALLENGE), eq(7L), anyInt());
    }

    @Test
    void 제공자_오류는_토큰_없이_실패로_알린다() {
        validState();
        when(client.fetchProfile(any(), any(), any(), any())).thenThrow(new IllegalStateException("boom"));

        assertThat(service.finish("kakao", "c", "s", null)).isEqualTo(APP + "?error=failed");
    }

    @Test
    void 코드를_바꾸면_JWT를_주고_두번째는_거절한다() {
        OAuthMapper.Ticket ticket = new OAuthMapper.Ticket();
        ticket.userId = 7L;
        ticket.codeChallenge = CHALLENGE;
        when(mapper.findTicket(anyString(), eq("LOGIN"))).thenReturn(ticket);
        when(mapper.deleteTicket(anyString())).thenReturn(1, 0);
        AuthResponse response = mock(AuthResponse.class);
        when(authService.issueToken(7L)).thenReturn(response);

        assertThat(service.exchange("code", VERIFIER)).isSameAs(response);
        assertThatThrownBy(() -> service.exchange("code", VERIFIER)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 만료되었거나_없는_코드는_거절한다() {
        when(mapper.findTicket(anyString(), eq("LOGIN"))).thenReturn(null);

        assertThatThrownBy(() -> service.exchange("code", VERIFIER)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.exchange(null, VERIFIER)).isInstanceOf(IllegalArgumentException.class);
        verify(authService, never()).issueToken(any());
    }

    @Test
    void 켜진_제공자_목록을_돌려준다() {
        when(client.enabled()).thenReturn(List.of("kakao"));

        assertThat(service.providers()).containsExactly("kakao");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-c=", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cMX"})
    void 형식이_틀린_challenge는_거절한다(String challenge) {
        assertThatThrownBy(() -> service.startUrl("kakao", APP, challenge)).isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).insertTicket(any(), any(), any(), any(), any(), any(), anyInt());
    }

    // 코드를 가로챈 다른 앱이나, 남이 보낸 링크를 연 사람은 verifier 가 없어 로그인할 수 없음
    @Test
    void verifier가_틀리면_토큰을_주지_않고_코드도_소모하지_않는다() {
        OAuthMapper.Ticket ticket = new OAuthMapper.Ticket();
        ticket.userId = 7L;
        ticket.codeChallenge = CHALLENGE;
        when(mapper.findTicket(anyString(), eq("LOGIN"))).thenReturn(ticket);

        assertThatThrownBy(() -> service.exchange("code", "wrong-verifier")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.exchange("code", null)).isInstanceOf(IllegalArgumentException.class);

        verify(authService, never()).issueToken(any());
        verify(mapper, never()).deleteTicket(anyString());
    }
}
