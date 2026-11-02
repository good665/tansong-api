package kr.tansong.demo.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
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

/** 사업자가 연락할 수 있도록, 전화번호가 없는 사용자는 신청할 때 입력해야 함 */
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
    void 전화번호가_이미_있으면_그대로_둔다() {
        userWithPhone("010-1111-2222");

        service.ensurePhone(1L, "010-9999-9999");

        verify(userMapper, never()).updatePhone(any(), any());
    }

    @Test
    void 전화번호가_없으면_입력값을_저장한다() {
        userWithPhone(null);

        assertThatCode(() -> service.ensurePhone(1L, " 010-1234-5678 ")).doesNotThrowAnyException();

        verify(userMapper).updatePhone(1L, "010-1234-5678");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "abc", "12", "010-1234-5678-9999-0000"})
    void 전화번호가_없는데_입력이_없거나_형식이_틀리면_거절한다(String contactPhone) {
        userWithPhone("  ");

        assertThatThrownBy(() -> service.ensurePhone(1L, contactPhone)).isInstanceOf(IllegalArgumentException.class);
        verify(userMapper, never()).updatePhone(any(), any());
    }
}
