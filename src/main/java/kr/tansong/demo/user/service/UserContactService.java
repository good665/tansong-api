package kr.tansong.demo.user.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/** 상담·예약을 받은 사업자가 연락할 수 있도록 신청자의 전화번호를 확보함 */
@Service
public class UserContactService {

    public static final Pattern PHONE = Pattern.compile("^[0-9\\-]{9,15}$");

    private final UserMapper userMapper;

    public UserContactService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 사용자에게 전화번호가 없으면 입력값을 검증해 저장. 이미 있으면 바꾸지 않음 */
    public void ensurePhone(Long userId, String contactPhone) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return;
        }

        String phone = contactPhone == null ? "" : contactPhone.trim();
        if (phone.isEmpty()) {
            throw new IllegalArgumentException("시설에서 연락받을 전화번호를 입력해 주세요.");
        }
        if (!PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("전화번호는 숫자와 하이픈(-)만 입력해 주세요.");
        }
        userMapper.updatePhone(userId, phone);
    }
}
