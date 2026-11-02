package kr.tansong.demo.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 제공자별 사용자 정보 응답 해석 */
class OAuthClientProfileTest {

    private static JsonNode json(String s) throws Exception {
        return new ObjectMapper().readTree(s);
    }

    @Test
    void 구글_응답을_해석한다() throws Exception {
        var p = OAuthClient.parseProfile("google", json("""
            {"sub":"g1","name":"구글","email":"a@b.com","email_verified":true}"""));

        assertThat(p).isEqualTo(new OAuthClient.Profile("g1", "구글", "a@b.com", true));
    }

    @Test
    void 카카오_숫자_id와_인증된_이메일을_해석한다() throws Exception {
        var p = OAuthClient.parseProfile("kakao", json("""
            {"id":123456789012,"kakao_account":{"email":"a@b.com","is_email_verified":true,"is_email_valid":true,
             "profile":{"nickname":"카카오"}}}"""));

        assertThat(p).isEqualTo(new OAuthClient.Profile("123456789012", "카카오", "a@b.com", true));
    }

    @Test
    void 카카오_이메일이_만료됐거나_동의하지_않으면_인증으로_보지_않는다() throws Exception {
        assertThat(OAuthClient.parseProfile("kakao", json("""
            {"id":1,"kakao_account":{"email":"a@b.com","is_email_verified":true,"is_email_valid":false}}""")).emailVerified()).isFalse();
        var none = OAuthClient.parseProfile("kakao", json("{\"id\":1}"));
        assertThat(none.email()).isNull();
        assertThat(none.emailVerified()).isFalse();
    }

    // 네이버는 이메일 인증 여부를 주지 않아 인증되지 않은 것으로 다룸
    @Test
    void 네이버_response_안의_값을_해석한다() throws Exception {
        var p = OAuthClient.parseProfile("naver", json("""
            {"resultcode":"00","message":"success","response":{"id":"n1","nickname":"별명","name":"이름","email":"a@b.com"}}"""));

        assertThat(p).isEqualTo(new OAuthClient.Profile("n1", "이름", "a@b.com", false));
    }

    @Test
    void 사용자_식별값이_없으면_실패한다() {
        assertThatThrownBy(() -> OAuthClient.parseProfile("naver", json("{\"resultcode\":\"024\"}")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OAuthClient.parseProfile("google", json("{\"sub\":\"\"}")))
                .isInstanceOf(IllegalStateException.class);
    }
}
