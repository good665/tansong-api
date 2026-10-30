package kr.tansong.demo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import kr.tansong.demo.admin.dto.OrganizationCreateRequest;
import kr.tansong.demo.auth.dto.AuthResponse;
import kr.tansong.demo.auth.dto.BusinessSignupRequest;
import kr.tansong.demo.auth.dto.LoginRequest;
import kr.tansong.demo.auth.security.JwtProvider;
import kr.tansong.demo.common.UnauthorizedException;
import kr.tansong.demo.facility.mapper.OrganizationMapper;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/**
 * 로그인 브루트포스 방지(실패 횟수 누적 -> 잠금 -> 잠금 중 비밀번호 미확인 -> 성공 시 초기화) 규칙과
 * 사업자 셀프 가입(signupBusiness: 계정 + 소유 기관을 함께 생성) 규칙을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrganizationMapper organizationMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userMapper, organizationMapper, passwordEncoder, jwtProvider, MAX_ATTEMPTS, LOCKOUT_MINUTES);
    }

    private BusinessSignupRequest businessSignupRequest() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setName("김사장");
        request.setEmail("owner@example.com");
        request.setPhone("010-1234-5678");
        request.setPassword("password1");
        request.setOrganizationName("테스트 요양 기관");
        request.setOrganizationType("CARE_PROVIDER");
        request.setBusinessRegistrationNumber("123-45-67890");
        return request;
    }

    private User userWith(int failedAttempts, LocalDateTime lockedUntil) {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setRole("USER");
        user.setFailedLoginAttempts(failedAttempts);
        user.setLockedUntil(lockedUntil);
        return user;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword(password);
        return request;
    }

    @Test
    void login_성공하면_이전_실패기록을_초기화하고_토큰을_발급한다() {
        User user = userWith(2, null);
        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.createToken(1L, "USER")).thenReturn("token-value");

        AuthResponse response = authService.login(loginRequest("correct-password"));

        assertThat(response.getToken()).isEqualTo("token-value");
        verify(userMapper).resetLoginFailure(1L);
        verify(userMapper, never()).updateLoginFailure(anyLong(), anyInt(), any());
    }

    @Test
    void login_비밀번호가_틀리면_실패횟수를_하나_올리고_거절한다() {
        User user = userWith(1, null);
        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("이메일 또는 비밀번호");

        verify(userMapper).updateLoginFailure(eq(1L), eq(2), isNull());
        verify(userMapper, never()).resetLoginFailure(anyLong());
    }

    @Test
    void login_실패횟수가_임계치에_도달하면_계정을_잠근다() {
        User user = userWith(MAX_ATTEMPTS - 1, null);
        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("wrong-password")))
                .isInstanceOf(UnauthorizedException.class);

        // 잠기는 순간 횟수는 0으로 리셋되고, lockedUntil 은 미래 시각으로 세팅됨
        verify(userMapper).updateLoginFailure(eq(1L), eq(0), any(LocalDateTime.class));
    }

    @Test
    void login_잠긴_계정은_비밀번호를_확인하지_않고_거절한다() {
        User user = userWith(0, LocalDateTime.now().plusMinutes(10));
        when(userMapper.findByEmail(anyString())).thenReturn(user);

        assertThatThrownBy(() -> authService.login(loginRequest("아무 비밀번호")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("잠겼습니다");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userMapper, never()).updateLoginFailure(anyLong(), anyInt(), any());
    }

    @Test
    void login_잠금시간이_지났으면_비밀번호_확인을_다시_진행한다() {
        User user = userWith(0, LocalDateTime.now().minusMinutes(1));
        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.createToken(anyLong(), anyString())).thenReturn("token-value");

        AuthResponse response = authService.login(loginRequest("correct-password"));

        assertThat(response.getToken()).isEqualTo("token-value");
    }

    @Test
    void login_존재하지_않는_이메일이면_공통_메시지로_거절하고_실패기록을_남기지_않는다() {
        when(userMapper.findByEmail(anyString())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(loginRequest("아무 비밀번호")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("이메일 또는 비밀번호");

        verify(userMapper, never()).updateLoginFailure(anyLong(), anyInt(), any());
    }

    @Test
    void signupBusiness_성공하면_역할이_BUSINESS_OWNER인_계정과_소유기관을_함께_생성한다() {
        when(userMapper.findByEmail(anyString())).thenReturn(null);
        // MyBatis 의 useGeneratedKeys 동작을 모사: insert 호출 시 넘겨진 User 객체에 id 를 채워 넣음
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(jwtProvider.createToken(42L, "BUSINESS_OWNER")).thenReturn("token-value");

        AuthResponse response = authService.signupBusiness(businessSignupRequest());

        assertThat(response.getToken()).isEqualTo("token-value");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo("BUSINESS_OWNER");

        ArgumentCaptor<OrganizationCreateRequest> orgCaptor =
                ArgumentCaptor.forClass(OrganizationCreateRequest.class);
        verify(organizationMapper).insert(orgCaptor.capture());
        assertThat(orgCaptor.getValue().getOwnerUserId()).isEqualTo(42L);
        assertThat(orgCaptor.getValue().getName()).isEqualTo("테스트 요양 기관");
        assertThat(orgCaptor.getValue().getBusinessRegistrationNumber()).isEqualTo("123-45-67890");
    }

    @Test
    void signupBusiness_사업자등록번호가_없으면_IllegalArgumentException을_던지고_계정을_만들지_않는다() {
        BusinessSignupRequest request = businessSignupRequest();
        request.setBusinessRegistrationNumber("");

        assertThatThrownBy(() -> authService.signupBusiness(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사업자등록번호");

        verify(userMapper, never()).insert(any());
        verify(organizationMapper, never()).insert(any());
    }

    @Test
    void signupBusiness_기관명이_없으면_IllegalArgumentException을_던진다() {
        BusinessSignupRequest request = businessSignupRequest();
        request.setOrganizationName("  ");

        assertThatThrownBy(() -> authService.signupBusiness(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("기관명");

        verify(userMapper, never()).insert(any());
    }
}
