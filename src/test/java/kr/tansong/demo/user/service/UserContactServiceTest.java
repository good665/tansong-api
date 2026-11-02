package kr.tansong.demo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/** 사업자가 연락할 수 있도록 신청할 때 전화번호를 확보하고, 잘못 입력한 번호는 고칠 수 있음 */
@ExtendWith(MockitoExtension.class)
class UserContactServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserContactService service;

    @BeforeEach
    void setUp() {
        service = new UserContactService(userMapper);
    }

    private void userWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        when(userMapper.findById(1L)).thenReturn(user);
    }

    @Test
    void 입력이_없으면_기존_번호를_그대로_둔다() {
        userWithPhone("010-1111-2222");

        service.ensurePhone(1L, null);

        verify(userMapper, never()).updatePhone(any(), any());
    }

    @Test
    void 같은_번호를_보내면_저장하지_않는다() {
        userWithPhone("010-1111-2222");

        service.ensurePhone(1L, " 010-1111-2222 ");

        verify(userMapper, never()).updatePhone(any(), any());
    }

    @Test
    void 잘못_입력한_번호는_새_번호로_고칠_수_있다() {
        userWithPhone("010-1234-567");

        service.ensurePhone(1L, "010-1234-5678");

        verify(userMapper).updatePhone(1L, "010-1234-5678");
    }

    @Test
    void 전화번호가_없으면_입력값을_저장한다() {
        userWithPhone(null);

        service.ensurePhone(1L, " 010-1234-5678 ");

        verify(userMapper).updatePhone(1L, "010-1234-5678");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void 전화번호가_없는데_입력도_없으면_거절한다(String contactPhone) {
        userWithPhone("  ");

        assertThatThrownBy(() -> service.ensurePhone(1L, contactPhone)).isInstanceOf(IllegalArgumentException.class);
        verify(userMapper, never()).updatePhone(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12", "---------", "000000000", "1012345678", "010-1234-5678-9999", "010--1234-5678"})
    void 전화번호_형식이_틀리면_기존_번호가_있어도_거절한다(String contactPhone) {
        userWithPhone("010-1111-2222");

        assertThatThrownBy(() -> service.ensurePhone(1L, contactPhone)).isInstanceOf(IllegalArgumentException.class);
        verify(userMapper, never()).updatePhone(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"010-1234-5678", "01012345678", "02-123-4567", "0211112222", "031-366-6585", "0507-1234-5678", "070-1234-5678"})
    void 국내_전화번호_형식은_통과한다(String phone) {
        assertThat(UserContactService.PHONE.matcher(phone).matches()).isTrue();
    }
}
