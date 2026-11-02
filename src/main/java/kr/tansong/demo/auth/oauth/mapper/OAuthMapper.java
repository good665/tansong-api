package kr.tansong.demo.auth.oauth.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 소셜 계정 연결(user_social_accounts)과 일회용 표(oauth_tickets: 해시만 저장) */
@Mapper
public interface OAuthMapper {

    class Ticket {
        public String provider;
        public String redirectUri;
        public String codeChallenge;
        public Long userId;
    }

    @Insert("""
        INSERT INTO oauth_tickets (token_hash, kind, provider, redirect_uri, code_challenge, user_id, expires_at)
        VALUES (#{hash}, #{kind}, #{provider}, #{redirectUri}, #{codeChallenge}, #{userId}, NOW() + INTERVAL #{seconds} SECOND)
        """)
    int insertTicket(@Param("hash") String hash, @Param("kind") String kind, @Param("provider") String provider,
                     @Param("redirectUri") String redirectUri, @Param("codeChallenge") String codeChallenge,
                     @Param("userId") Long userId,
                     @Param("seconds") int seconds);

    @Select("""
        SELECT provider, redirect_uri AS redirectUri, code_challenge AS codeChallenge, user_id AS userId
          FROM oauth_tickets
         WHERE token_hash = #{hash} AND kind = #{kind} AND expires_at > NOW()
        """)
    Ticket findTicket(@Param("hash") String hash, @Param("kind") String kind);

    // 지운 행이 1일 때만 사용 성공: 같은 표를 동시에 써도 한쪽만 통과
    @Delete("DELETE FROM oauth_tickets WHERE token_hash = #{hash}")
    int deleteTicket(@Param("hash") String hash);

    @Delete("DELETE FROM oauth_tickets WHERE expires_at <= NOW()")
    int deleteExpired();

    @Select("SELECT user_id FROM user_social_accounts WHERE provider = #{provider} AND provider_user_id = #{providerUserId}")
    Long findLinkedUser(@Param("provider") String provider, @Param("providerUserId") String providerUserId);

    @Insert("INSERT INTO user_social_accounts (provider, provider_user_id, user_id) VALUES (#{provider}, #{providerUserId}, #{userId})")
    int insertLink(@Param("provider") String provider, @Param("providerUserId") String providerUserId,
                   @Param("userId") Long userId);
}
