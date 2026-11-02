package kr.tansong.demo.auth.oauth;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.auth.dto.AuthResponse;

/** 소셜 로그인 (비로그인 공개). 자세한 흐름은 OAuthService 참고 */
@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthService service;

    public OAuthController(OAuthService service) {
        this.service = service;
    }

    // 키가 설정된 제공자만 앱에 버튼으로 표시
    @GetMapping("/providers")
    public List<String> providers() {
        return service.providers();
    }

    @GetMapping("/{provider}/start")
    public ResponseEntity<Void> start(@PathVariable String provider, @RequestParam String redirect) {
        return redirect(service.startUrl(provider, redirect));
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        return redirect(service.finish(provider, code, state, error));
    }

    @PostMapping("/exchange")
    public AuthResponse exchange(@RequestBody Map<String, String> body) {
        return service.exchange(body == null ? null : body.get("code"));
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
