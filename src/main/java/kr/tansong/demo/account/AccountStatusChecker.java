package kr.tansong.demo.account;

import org.springframework.stereotype.Component;

import kr.tansong.demo.account.mapper.AccountMapper;

/** JWT 는 서명만으로 유효하므로, 탈퇴했거나 비밀번호를 바꾸기 전에 받은 토큰을 여기서 막음 */
@Component
public class AccountStatusChecker {

    private final AccountMapper mapper;

    public AccountStatusChecker(AccountMapper mapper) {
        this.mapper = mapper;
    }

    public boolean isValid(Long userId, long issuedAtSeconds) {
        AccountMapper.AccountStatus status = mapper.findAccountStatus(userId);
        if (status == null) {
            return true;
        }
        return !status.deleted && (status.passwordChangedAt == null || issuedAtSeconds >= status.passwordChangedAt);
    }
}
