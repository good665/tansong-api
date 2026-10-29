package kr.tansong.demo.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:604800000}") long expirationMs) {

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        // 시크릿이 짧거나 비어 있으면 서버 시작 단계에서 바로 실패시킴
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 은 32바이트 이상이어야 합니다.");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String createToken(Long userId, String role) {
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** 유효하지 않으면 JwtException 발생 */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
