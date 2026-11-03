package kr.tansong.demo.common;

/** 비밀번호를 너무 많이 틀려 잠긴 계정. 401 이면 앱이 로그아웃시키므로 따로 둠 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
