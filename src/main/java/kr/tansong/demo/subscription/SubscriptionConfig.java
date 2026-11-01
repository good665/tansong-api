package kr.tansong.demo.subscription;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 구독 기간은 한국 날짜 기준으로 끊습니다 (만료 다음날 00:00 KST 에 상단에서 내려감).
 *  DB 쪽 노출 판정은 MySQL NOW() 를 쓰므로 DB 서버 시간대도 KST 여야 합니다. */
@Configuration
@EnableScheduling
public class SubscriptionConfig {

    public static final String ZONE = "Asia/Seoul";

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(ZONE));
    }
}
