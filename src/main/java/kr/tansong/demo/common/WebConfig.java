package kr.tansong.demo.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 개발용 전역 CORS 설정.
 * Expo 웹은 보통 8081 포트라서 FacilityController의 localhost:3000 설정으로는 막힙니다.
 * 이 클래스를 추가한 뒤 각 컨트롤러의 @CrossOrigin은 지워도 됩니다.
 * (배포 시에는 allowedOriginPatterns를 실제 도메인으로 제한하세요)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
    }
}