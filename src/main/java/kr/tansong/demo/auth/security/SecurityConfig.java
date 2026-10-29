package kr.tansong.demo.auth.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {
        http
            .csrf(csrf -> csrf.disable())              // 쿠키가 아닌 Bearer 토큰 방식이라 CSRF 불필요
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/facilities/**").permitAll()   // 시설 조회는 비로그인 허용
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()          // 업로드된 사진은 비로그인 조회 허용
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")                   // 관리자 전용
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"success\":false,\"message\":\"관리자만 접근할 수 있습니다.\"}");
                }))
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 기존 WebConfig(MVC CORS)를 대체합니다. WebConfig.java 는 삭제하세요.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:*}") String origins) {

        List<String> patterns = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
