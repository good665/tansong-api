package kr.tansong.demo.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.account.mapper.AccountMapper;
import kr.tansong.demo.auth.security.JwtProvider;
import kr.tansong.demo.auth.service.AuthService;
import kr.tansong.demo.auth.service.PasswordVerifier;
import kr.tansong.demo.common.UnauthorizedException;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/** 로그인한 사용자의 이름·전화번호 수정과 비밀번호 변경. 이메일은 로그인 아이디라 바꾸지 않음 */
@Service
public class AccountProfileService {

    private final AccountMapper mapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordVerifier passwordVerifier;
    private final JwtProvider jwtProvider;
    private final TransactionTemplate tx;

    public AccountProfileService(AccountMapper mapper, UserMapper userMapper, PasswordEncoder passwordEncoder,
                                 PasswordVerifier passwordVerifier, JwtProvider jwtProvider, TransactionTemplate tx) {
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordVerifier = passwordVerifier;
        this.jwtProvider = jwtProvider;
        this.tx = tx;
    }

    public User updateProfile(Long userId, String rawName, String rawPhone) {
        User user = findUser(userId);
        String name = rawName == null ? "" : rawName.trim();
        String phone = rawPhone == null ? "" : rawPhone.trim();
        AuthService.validateNameAndPhone(name, phone);

        user.setName(name);
        user.setPhone(phone.isEmpty() ? null : phone);
        userMapper.updateProfile(userId, user.getName(), user.getPhone());
        return user;
    }

    /** 다른 기기의 토큰은 무효가 되므로, 지금 기기가 계속 쓸 새 토큰을 돌려줌 */
    public String changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (mapper.hasSocialAccount(userId)) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }
        AuthService.validatePassword(newPassword);
        if (!passwordVerifier.verify(user, currentPassword)) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        String encoded = passwordEncoder.encode(newPassword);
        tx.executeWithoutResult(status -> {
            userMapper.updatePassword(userId, encoded);
            mapper.markPasswordChanged(userId);
            mapper.deletePasswordResetCode(userId);
        });
        return jwtProvider.createToken(userId, user.getRole());
    }

    private User findUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new UnauthorizedException("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
        }
        return user;
    }
}
