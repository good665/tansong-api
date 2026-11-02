package kr.tansong.demo.user.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/** 상담·예약을 받은 사업자가 연락할 수 있도록 신청자의 전화번호를 확보함 */
@Service
public class UserContactService {

    public static final Pattern PHONE = Pattern.compile("^0[1-9]\\d{0,2}-?\\d{3,4}-?\\d{4}$");

    private final UserMapper userMapper;

    public UserContactService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 입력값이 있으면 검증해 저장(잘못 입력한 번호를 고칠 수 있음). 없으면 기존 번호가 있어야 함 */
    public void ensurePhone(Long userId, String contactPhone) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
        }
        String phone = contactPhone == null ? "" : contactPhone.trim();
        String current = user.getPhone() == null ? "" : user.getPhone().trim();
        if (phone.isEmpty()) {
            if (current.isEmpty()) {
                throw new IllegalArgumentException("시설에서 연락받을 전화번호를 입력해 주세요.");
            }
            return;
        }
        if (!PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다. 예: 010-1234-5678");
        }
        if (!phone.equals(current)) {
            userMapper.updatePhone(userId, phone);
        }
    }
}
