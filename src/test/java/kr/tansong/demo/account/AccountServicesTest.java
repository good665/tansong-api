package kr.tansong.demo.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.auth.service.PasswordVerifier;
import kr.tansong.demo.common.AccountLockedException;
import kr.tansong.demo.auth.security.JwtProvider;
import kr.tansong.demo.account.mapper.AccountMapper;
import kr.tansong.demo.common.ServiceUnavailableException;
import kr.tansong.demo.facilityclaim.ClaimDocumentStorage;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServicesTest {

    private static final Long USER = 1L;
    private static final String EMAIL = "a@b.com";
    private static final String NEW_PASSWORD = "Newpass123";

    @Mock
    private AccountMapper mapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private ClaimDocumentStorage storage;
    @Mock
    private PasswordResetMailer mailer;

    private final TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private PasswordVerifier verifier;

    private User user(String role) {
        User user = new User();
        user.setId(USER);
        user.setEmail(EMAIL);
        user.setRole(role);
        user.setPasswordHash("stored");
        when(userMapper.findById(USER)).thenReturn(user);
        when(userMapper.findByEmail(EMAIL)).thenReturn(user);
        return user;
    }

    @BeforeEach
    void defaults() {
        verifier = new PasswordVerifier(userMapper, encoder, 5, 15);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(mailer.isConfigured()).thenReturn(true);
    }

    @Nested
    class Profile {

        @Mock
        private JwtProvider jwtProvider;
        private AccountProfileService service;

        @BeforeEach
        void setUp() {
            service = new AccountProfileService(mapper, userMapper, encoder, verifier, jwtProvider, tx);
            when(encoder.matches("right", "stored")).thenReturn(true);
        }

        @Test
        void 이름과_전화번호를_다듬어_저장한다() {
            user("USER");

            User saved = service.updateProfile(USER, " 홍길동 ", " 010-1234-5678 ");

            verify(userMapper).updateProfile(USER, "홍길동", "010-1234-5678");
            assertThat(saved.getName()).isEqualTo("홍길동");
        }

        @Test
        void 전화번호를_비우면_NULL로_저장한다() {
            user("USER");

            service.updateProfile(USER, "홍길동", "  ");

            verify(userMapper).updateProfile(USER, "홍길동", null);
        }

        @Test
        void 이름이_없거나_전화번호_형식이_틀리면_저장하지_않는다() {
            user("USER");

            assertThatThrownBy(() -> service.updateProfile(USER, " ", "")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.updateProfile(USER, null, "")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.updateProfile(USER, "홍길동", "abc")).isInstanceOf(IllegalArgumentException.class);
            verify(userMapper, never()).updateProfile(any(), any(), any());
        }

        @Test
        void 현재_비밀번호가_맞으면_바꾸고_이전_토큰을_막고_새_토큰을_준다() {
            user("USER");
            when(jwtProvider.createToken(USER, "USER")).thenReturn("new-token");

            assertThat(service.changePassword(USER, "right", NEW_PASSWORD)).isEqualTo("new-token");

            verify(userMapper).updatePassword(USER, "encoded");
            verify(mapper).markPasswordChanged(USER);
            verify(mapper).deletePasswordResetCode(USER);
        }

        @Test
        void 현재_비밀번호가_틀리거나_없으면_바꾸지_않는다() {
            user("USER");

            assertThatThrownBy(() -> service.changePassword(USER, "wrong", NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.changePassword(USER, null, NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);
            verify(userMapper, never()).updatePassword(any(), any());
            verify(mapper, never()).markPasswordChanged(any());
        }

        @Test
        void 약한_새_비밀번호는_거절한다() {
            user("USER");

            assertThatThrownBy(() -> service.changePassword(USER, "right", "short")).isInstanceOf(IllegalArgumentException.class);
            verify(userMapper, never()).updatePassword(any(), any());
        }

        @Test
        void 틀리면_실패_횟수를_올린다() {
            user("USER");

            assertThatThrownBy(() -> service.changePassword(USER, "wrong", NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);

            verify(userMapper).updateLoginFailure(USER, 1, null);
        }

        @Test
        void 다섯_번째로_틀리면_잠근다() {
            user("USER").setFailedLoginAttempts(4);

            assertThatThrownBy(() -> service.changePassword(USER, "wrong", NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);

            verify(userMapper).updateLoginFailure(eq(USER), eq(0), any(LocalDateTime.class));
        }

        @Test
        void 잠긴_계정은_맞는_비밀번호도_확인하지_않는다() {
            user("USER").setLockedUntil(LocalDateTime.now().plusMinutes(10));

            assertThatThrownBy(() -> service.changePassword(USER, "right", NEW_PASSWORD)).isInstanceOf(AccountLockedException.class);

            verify(encoder, never()).matches(any(), any());
            verify(userMapper, never()).updatePassword(any(), any());
        }

        @Test
        void 맞으면_실패_횟수를_지운다() {
            user("USER").setFailedLoginAttempts(3);

            service.changePassword(USER, "right", NEW_PASSWORD);

            verify(userMapper).resetLoginFailure(USER);
        }

        @Test
        void 약한_새_비밀번호는_횟수를_쓰지_않는다() {
            user("USER");

            assertThatThrownBy(() -> service.changePassword(USER, "wrong", "short")).isInstanceOf(IllegalArgumentException.class);

            verify(userMapper, never()).updateLoginFailure(any(), anyInt(), any());
        }

        @Test
        void 소셜_계정은_비밀번호를_바꿀_수_없다() {
            user("USER");
            when(mapper.hasSocialAccount(USER)).thenReturn(true);

            assertThatThrownBy(() -> service.changePassword(USER, "right", NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);
            verify(userMapper, never()).updatePassword(any(), any());
        }
    }

    @Nested
    class Withdrawal {

        private WithdrawalService service;

        @BeforeEach
        void setUp() {
            service = new WithdrawalService(mapper, userMapper, encoder, verifier, storage, tx);
            when(encoder.matches("right", "stored")).thenReturn(true);
        }

        @Test
        void 비밀번호가_틀리면_아무것도_바꾸지_않는다() {
            user("USER");

            assertThatThrownBy(() -> service.withdraw(USER, "wrong")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.withdraw(USER, null)).isInstanceOf(IllegalArgumentException.class);

            verify(mapper, never()).anonymizeUser(anyLong(), anyString(), anyString());
        }

        @Test
        void 틀리면_실패_횟수를_올리고_입력이_없으면_세지_않는다() {
            user("USER");

            assertThatThrownBy(() -> service.withdraw(USER, null)).isInstanceOf(IllegalArgumentException.class);
            verify(userMapper, never()).updateLoginFailure(any(), anyInt(), any());

            assertThatThrownBy(() -> service.withdraw(USER, "wrong")).isInstanceOf(IllegalArgumentException.class);
            verify(userMapper).updateLoginFailure(USER, 1, null);
        }

        @Test
        void 잠긴_계정은_맞는_비밀번호로도_탈퇴할_수_없다() {
            user("USER").setLockedUntil(LocalDateTime.now().plusMinutes(10));

            assertThatThrownBy(() -> service.withdraw(USER, "right")).isInstanceOf(AccountLockedException.class);

            verify(mapper, never()).markDeleted(any());
        }

        @Test
        void 소셜_계정은_비밀번호_없이_탈퇴한다() {
            user("USER");
            when(mapper.hasSocialAccount(USER)).thenReturn(true);

            service.withdraw(USER, null);

            verify(mapper).deleteSocialAccounts(USER);
            verify(mapper).anonymizeUser(eq(USER), anyString(), anyString());
        }

        @Test
        void 관리자는_앱에서_탈퇴할_수_없다() {
            user("ADMIN");

            assertThatThrownBy(() -> service.withdraw(USER, "right")).isInstanceOf(IllegalArgumentException.class);
            verify(mapper, never()).anonymizeUser(anyLong(), anyString(), anyString());
        }

        @Test
        void 개인정보를_익명화하고_진행중인_신청을_취소하고_토큰을_막는다() {
            user("USER");

            service.withdraw(USER, "right");

            verify(mapper).cancelMyConsultations(USER);
            verify(mapper).cancelMyReservations(USER);
            verify(mapper).deletePasswordResetCode(USER);
            verify(mapper).markDeleted(USER);
            ArgumentCaptor<String> email = ArgumentCaptor.forClass(String.class);
            verify(mapper).anonymizeUser(eq(USER), email.capture(), eq("encoded"));
            assertThat(email.getValue()).isEqualTo("deleted_1@deleted.invalid");
        }

        @Test
        void 사업자는_자동결제를_끄고_시설을_내린_뒤_소유를_끊는다() {
            user("BUSINESS_OWNER");

            service.withdraw(USER, "right");

            var order = inOrder(mapper);
            // 소유 관계로 찾으므로 소유를 끊는 일이 맨 마지막이어야 함
            order.verify(mapper).cancelOwnedSubscriptions(USER);
            order.verify(mapper).deleteOwnedBillingCards(USER);
            order.verify(mapper).cancelOwnedFacilityConsultations(USER);
            order.verify(mapper).cancelOwnedFacilityReservations(USER);
            order.verify(mapper).deactivateOwnedFacilities(USER);
            order.verify(mapper).releaseOrganizations(USER);
        }

        @Test
        void 대기중인_소유권_신청의_서류는_확정_후_지운다() {
            user("BUSINESS_OWNER");
            when(mapper.findPendingClaimDocuments(USER)).thenReturn(List.of("doc.pdf"));

            service.withdraw(USER, "right");

            verify(mapper).rejectPendingClaims(eq(USER), anyString());
            verify(storage).delete("doc.pdf");
        }

        @Test
        void 처리_중_실패하면_서류를_지우지_않는다() {
            user("BUSINESS_OWNER");
            when(mapper.findPendingClaimDocuments(USER)).thenReturn(List.of("doc.pdf"));
            when(mapper.markDeleted(USER)).thenThrow(new IllegalStateException("db"));

            assertThatThrownBy(() -> service.withdraw(USER, "right")).isInstanceOf(IllegalStateException.class);
            verify(storage, never()).delete(any());
        }
    }

    @Nested
    class PasswordReset {

        private PasswordResetService service;

        @BeforeEach
        void setUp() {
            service = new PasswordResetService(mapper, userMapper, encoder, mailer, tx, Runnable::run, "test-secret");
        }

        private String requestCode() {
            user("USER");
            service.request(EMAIL);
            ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
            verify(mailer).send(eq(EMAIL), code.capture());
            return code.getValue();
        }

        private AccountMapper.ResetCode stored(String hash, int attempts) {
            AccountMapper.ResetCode row = new AccountMapper.ResetCode();
            row.codeHash = hash;
            row.attempts = attempts;
            when(mapper.findValidResetCode(USER)).thenReturn(row);
            return row;
        }

        private String storedHash() {
            ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
            verify(mapper).saveResetCode(eq(USER), hash.capture(), anyInt());
            return hash.getValue();
        }

        @Test
        void 메일이_설정되지_않으면_가입_여부와_무관하게_알린다() {
            when(mailer.isConfigured()).thenReturn(false);

            assertThatThrownBy(() -> service.request("nobody@b.com")).isInstanceOf(ServiceUnavailableException.class);
        }

        @Test
        void 코드는_6자리이고_해시로만_저장한다() {
            String code = requestCode();

            assertThat(code).matches("\\d{6}");
            assertThat(storedHash()).isNotEqualTo(code).hasSizeGreaterThan(30);
        }

        @Test
        void 없는_이메일과_소셜_계정은_조용히_끝나_가입_여부가_드러나지_않는다() {
            assertThatCode(() -> service.request("nobody@b.com")).doesNotThrowAnyException();

            user("USER");
            when(mapper.hasSocialAccount(USER)).thenReturn(true);
            assertThatCode(() -> service.request(EMAIL)).doesNotThrowAnyException();

            verify(mailer, never()).send(any(), any());
        }

        @Test
        void 탈퇴한_계정에는_코드를_보내지_않는다() {
            user("USER");
            AccountMapper.AccountStatus deleted = new AccountMapper.AccountStatus();
            deleted.deleted = true;
            when(mapper.findAccountStatus(USER)).thenReturn(deleted);

            service.request(EMAIL);

            verify(mapper, never()).saveResetCode(any(), any(), anyInt());
            verify(mailer, never()).send(any(), any());
        }

        @Test
        void 방금_보냈으면_다시_보내지_않는다() {
            user("USER");
            when(mapper.hasRecentResetCode(USER, 60)).thenReturn(true);

            service.request(EMAIL);

            verify(mailer, never()).send(any(), any());
        }

        @Test
        void 메일_발송_실패가_요청_실패로_드러나지_않는다() {
            user("USER");
            org.mockito.Mockito.doThrow(new IllegalStateException("smtp")).when(mailer).send(any(), any());

            assertThatCode(() -> service.request(EMAIL)).doesNotThrowAnyException();
        }

        @Test
        void 맞는_코드면_비밀번호를_바꾸고_기존_토큰을_막고_코드를_지운다() {
            String code = requestCode();
            stored(storedHash(), 0);

            service.confirm(EMAIL, code, NEW_PASSWORD);

            verify(userMapper).updatePassword(USER, "encoded");
            verify(userMapper).resetLoginFailure(USER);
            verify(mapper).markPasswordChanged(USER);
            verify(mapper).deletePasswordResetCode(USER);
        }

        @Test
        void 틀린_코드는_시도_횟수를_올리고_비밀번호를_바꾸지_않는다() {
            String code = requestCode();
            stored(storedHash(), 0);
            String wrong = code.equals("000000") ? "000001" : "000000";

            assertThatThrownBy(() -> service.confirm(EMAIL, wrong, NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);

            verify(mapper).incrementResetAttempts(USER);
            verify(userMapper, never()).updatePassword(any(), any());
        }

        @Test
        void 다섯_번_틀리면_맞는_코드도_거절한다() {
            String code = requestCode();
            stored(storedHash(), 5);

            assertThatThrownBy(() -> service.confirm(EMAIL, code, NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);

            verify(userMapper, never()).updatePassword(any(), any());
        }

        @Test
        void 없거나_만료된_코드와_없는_이메일은_같은_문구로_거절한다() {
            user("USER");
            when(mapper.findValidResetCode(USER)).thenReturn(null);

            String expired = message(() -> service.confirm(EMAIL, "123456", NEW_PASSWORD));
            String unknown = message(() -> service.confirm("nobody@b.com", "123456", NEW_PASSWORD));

            assertThat(expired).isEqualTo(unknown);
            verify(userMapper, never()).updatePassword(any(), any());
        }

        private String message(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
            try {
                call.call();
            } catch (Throwable t) {
                return t.getMessage();
            }
            throw new AssertionError("예외가 발생해야 합니다");
        }

        @Test
        void 약한_새_비밀번호는_코드를_소모하지_않고_거절한다() {
            String code = requestCode();
            stored(storedHash(), 0);

            assertThatThrownBy(() -> service.confirm(EMAIL, code, "short")).isInstanceOf(IllegalArgumentException.class);

            verify(mapper, times(0)).incrementResetAttempts(any());
        }

        @Test
        void 형식이_틀린_코드는_조회하지_않는다() {
            user("USER");

            assertThatThrownBy(() -> service.confirm(EMAIL, "abc", NEW_PASSWORD)).isInstanceOf(IllegalArgumentException.class);
            verify(mapper, never()).findValidResetCode(any());
        }
    }

    @Nested
    class TokenCheck {

        private AccountStatusChecker checker;

        @BeforeEach
        void setUp() {
            checker = new AccountStatusChecker(mapper);
        }

        private void status(boolean deleted, Long changedEpoch) {
            AccountMapper.AccountStatus row = new AccountMapper.AccountStatus();
            row.deleted = deleted;
            row.passwordChangedAt = changedEpoch;
            when(mapper.findAccountStatus(USER)).thenReturn(row);
        }

        @Test
        void 기록이_없으면_유효하다() {
            when(mapper.findAccountStatus(USER)).thenReturn(null);

            assertThat(checker.isValid(USER, 1000L)).isTrue();
        }

        @Test
        void 탈퇴한_계정의_토큰은_무효다() {
            status(true, null);

            assertThat(checker.isValid(USER, 1000L)).isFalse();
        }

        @Test
        void 비밀번호_변경_전에_받은_토큰만_무효다() {
            status(false, 1000L);

            assertThat(checker.isValid(USER, 999L)).isFalse();
            assertThat(checker.isValid(USER, 1000L)).isTrue();
        }
    }
}
