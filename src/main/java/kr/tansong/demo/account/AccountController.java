package kr.tansong.demo.account;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 회원 탈퇴(로그인 필요)와 비밀번호 찾기(로그인 전, SecurityConfig 에서 공개) */
@RestController
public class AccountController {

    private final WithdrawalService withdrawalService;
    private final PasswordResetService passwordResetService;

    public AccountController(WithdrawalService withdrawalService, PasswordResetService passwordResetService) {
        this.withdrawalService = withdrawalService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/api/account")
    public Map<String, Boolean> account(@AuthenticationPrincipal Long userId) {
        return Map.of("socialOnly", withdrawalService.isSocialOnly(userId));
    }

    @PostMapping("/api/account/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal Long userId, @RequestBody(required = false) Map<String, String> body) {
        withdrawalService.withdraw(userId, body == null ? null : body.get("password"));
        return ResponseEntity.ok(Map.of("success", true, "message", "탈퇴가 완료되었습니다."));
    }

    @PostMapping("/api/auth/password-reset/request")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        passwordResetService.request(body.get("email"));
        // 가입 여부와 무관하게 같은 응답
        return ResponseEntity.ok(Map.of("success", true, "message", "가입된 이메일이라면 재설정 코드를 보냈습니다."));
    }

    @PostMapping("/api/auth/password-reset/confirm")
    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> body) {
        passwordResetService.confirm(body.get("email"), body.get("code"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호를 변경했습니다."));
    }
}
