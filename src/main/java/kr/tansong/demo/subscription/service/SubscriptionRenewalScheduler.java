package kr.tansong.demo.subscription.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import kr.tansong.demo.subscription.SubscriptionConfig;

/** 매일 23:50(KST)에 오늘로 기간이 끝나는 자동 연장 구독을 청구합니다.
 *  자정 전에 연장해 두어야 다음날 00:00 에 상단에서 잠깐 내려가는 공백이 생기지 않습니다. */
@Component
public class SubscriptionRenewalScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRenewalScheduler.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionRenewalScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 50 23 * * *", zone = SubscriptionConfig.ZONE)
    public void renewDueSubscriptions() {
        runOnce();
    }

    /** @return 연장에 성공한 건수 */
    public int runOnce() {
        int renewed = 0;

        for (Long facilityId : subscriptionService.findDueForRenewal()) {
            try {
                if (subscriptionService.renew(facilityId)) {
                    renewed++;
                }
            } catch (Exception e) {
                // 한 시설이 실패해도 나머지 시설의 연장은 계속 진행
                log.error("구독 자동 연장 실패 facilityId={}", facilityId, e);
            }
        }

        log.info("구독 자동 연장 완료: {}건", renewed);
        return renewed;
    }
}
