package kr.tansong.demo.auth.oauth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 구글·네이버·카카오 OAuth 호출 (인가 코드 방식). 시크릿과 토큰은 서버에만 있고 앱으로 보내지 않습니다.
 * 키(client-id)가 설정된 제공자만 켜집니다.
 */
@Component
public class OAuthClient {

    public record Profile(String providerUserId, String name, String email, boolean emailVerified) {}

    private record Provider(String clientId, String clientSecret, String authorizeUrl, String tokenUrl,
                            String userInfoUrl, String scope) {}

    private final Map<String, Provider> providers = new LinkedHashMap<>();
    private final RestClient restClient;
    private final String publicBaseUrl;

    public OAuthClient(RestClient.Builder builder,
                       @Value("${app.oauth.public-base-url}") String publicBaseUrl,
                       @Value("${app.oauth.google.client-id:}") String googleId,
                       @Value("${app.oauth.google.client-secret:}") String googleSecret,
                       @Value("${app.oauth.google.authorize-url}") String googleAuthorize,
                       @Value("${app.oauth.google.token-url}") String googleToken,
                       @Value("${app.oauth.google.user-info-url}") String googleUserInfo,
                       @Value("${app.oauth.naver.client-id:}") String naverId,
                       @Value("${app.oauth.naver.client-secret:}") String naverSecret,
                       @Value("${app.oauth.naver.authorize-url}") String naverAuthorize,
                       @Value("${app.oauth.naver.token-url}") String naverToken,
                       @Value("${app.oauth.naver.user-info-url}") String naverUserInfo,
                       @Value("${app.oauth.kakao.client-id:}") String kakaoId,
                       @Value("${app.oauth.kakao.client-secret:}") String kakaoSecret,
                       @Value("${app.oauth.kakao.authorize-url}") String kakaoAuthorize,
                       @Value("${app.oauth.kakao.token-url}") String kakaoToken,
                       @Value("${app.oauth.kakao.user-info-url}") String kakaoUserInfo) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = builder.requestFactory(factory).build();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");

        // 네이버·카카오 scope 는 개발자 콘솔의 동의 항목을 따름
        add("google", googleId, googleSecret, googleAuthorize, googleToken, googleUserInfo, "openid email profile");
        add("naver", naverId, naverSecret, naverAuthorize, naverToken, naverUserInfo, null);
        add("kakao", kakaoId, kakaoSecret, kakaoAuthorize, kakaoToken, kakaoUserInfo, null);
    }

    private void add(String name, String id, String secret, String authorize, String token, String userInfo, String scope) {
        if (id != null && !id.isBlank()) {
            providers.put(name, new Provider(id.trim(), secret == null ? "" : secret.trim(), authorize, token, userInfo, scope));
        }
    }

    public List<String> enabled() {
        return List.copyOf(providers.keySet());
    }

    public boolean isEnabled(String provider) {
        return providers.containsKey(provider);
    }

    public String callbackUrl(String provider) {
        return publicBaseUrl + "/api/auth/oauth/" + provider + "/callback";
    }

    public String authorizeUrl(String provider, String state, String callbackUrl) {
        Provider p = providers.get(provider);
        UriComponentsBuilder url = UriComponentsBuilder.fromHttpUrl(p.authorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", p.clientId())
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("state", state);
        if (p.scope() != null) {
            url.queryParam("scope", p.scope());
        }
        return url.encode().build().toUriString();
    }

    public Profile fetchProfile(String provider, String code, String state, String callbackUrl) {
        Provider p = providers.get(provider);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", p.clientId());
        form.add("redirect_uri", callbackUrl);
        form.add("code", code);
        if (!p.clientSecret().isEmpty()) {
            form.add("client_secret", p.clientSecret());
        }
        if ("naver".equals(provider)) {
            form.add("state", state);
        }

        JsonNode token = restClient.post().uri(p.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        // 네이버는 실패도 200 으로 응답할 수 있음
        String accessToken = text(token, "access_token");
        if (accessToken == null) {
            throw new IllegalStateException(provider + " 토큰 발급 실패");
        }

        JsonNode userInfo = restClient.get().uri(p.userInfoUrl())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);
        return parseProfile(provider, userInfo);
    }

    static Profile parseProfile(String provider, JsonNode json) {
        Profile profile = switch (provider) {
            case "google" -> new Profile(text(json, "sub"), text(json, "name"), text(json, "email"),
                    json.path("email_verified").asBoolean(false));
            case "kakao" -> {
                JsonNode account = json.path("kakao_account");
                yield new Profile(text(json, "id"), text(account.path("profile"), "nickname"), text(account, "email"),
                        account.path("is_email_verified").asBoolean(false)
                                && account.path("is_email_valid").asBoolean(false));
            }
            case "naver" -> {
                JsonNode r = json.path("response");
                String name = text(r, "name");
                // 네이버는 이메일 인증 여부를 주지 않으므로 인증된 것으로 보지 않음
                yield new Profile(text(r, "id"), name != null ? name : text(r, "nickname"), text(r, "email"), false);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 로그인 방식입니다.");
        };
        if (profile.providerUserId() == null) {
            throw new IllegalStateException(provider + " 사용자 식별값 없음");
        }
        return profile;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }
}
