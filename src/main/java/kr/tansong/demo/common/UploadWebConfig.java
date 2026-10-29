package kr.tansong.demo.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 업로드된 사진을 /uploads/** 로 공개 (조회만 허용: SecurityConfig 참고) */
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    private final ImageStorageService storage;

    public UploadWebConfig(ImageStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + storage.getRoot().toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
