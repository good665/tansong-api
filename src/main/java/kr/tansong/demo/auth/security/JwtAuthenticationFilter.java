package kr.tansong.demo.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization: Bearer <token> 을 읽어 인증 정보를 채웁니다.
 * principal 은 userId(Long) 이므로 컨트롤러에서 @AuthenticationPrincipal Long userId 로 받습니다.
 * @Component 를 붙이지 않고 SecurityConfig 에서 직접 생성합니다(서블릿 필터로 이중 등록되는 것을 방지).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtProvider.parse(header.substring(7));
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);

                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + (role == null ? "USER" : role))));

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                // 만료/위조 토큰: 인증 없이 진행 -> 보호된 API는 401, 공개 API는 그대로 통과
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
