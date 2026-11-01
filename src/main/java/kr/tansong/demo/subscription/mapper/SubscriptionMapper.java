package kr.tansong.demo.subscription.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.tansong.demo.subscription.domain.Subscription;
import kr.tansong.demo.subscription.dto.SubscriptionPaymentResponse;

/** 시설의 상단노출(광고) 구독과 결제 이력. 테이블 정의는 resources/schema.sql.
 *  toss_billing_key/toss_customer_key 컬럼은 실제 PG 정기결제를 붙일 때 채울 자리입니다. */
@Mapper
public interface SubscriptionMapper {

    @Select("""
        SELECT facility_id AS facilityId, plan, status, current_period_end AS currentPeriodEnd
          FROM subscriptions
         WHERE facility_id = #{facilityId}
        """)
    Subscription findByFacilityId(@Param("facilityId") Long facilityId);

    // 결제/연장 중 같은 시설에 대한 중복 요청(버튼 연타 등)이 동시에 처리되지 않도록 행 잠금
    @Select("""
        SELECT facility_id AS facilityId, plan, status, current_period_end AS currentPeriodEnd
          FROM subscriptions
         WHERE facility_id = #{facilityId}
           FOR UPDATE
        """)
    Subscription findByFacilityIdForUpdate(@Param("facilityId") Long facilityId);

    // facility_id 가 UNIQUE 라 한 시설당 구독 레코드는 항상 하나 — upsert
    @Insert("""
        INSERT INTO subscriptions (facility_id, plan, status, current_period_end)
        VALUES (#{facilityId}, #{plan}, #{status}, #{currentPeriodEnd})
        ON DUPLICATE KEY UPDATE
            plan = #{plan},
            status = #{status},
            current_period_end = #{currentPeriodEnd}
        """)
    int upsert(@Param("facilityId") Long facilityId,
               @Param("plan") String plan,
               @Param("status") String status,
               @Param("currentPeriodEnd") LocalDateTime currentPeriodEnd);

    @Update("UPDATE subscriptions SET status = #{status} WHERE facility_id = #{facilityId}")
    int updateStatus(@Param("facilityId") Long facilityId, @Param("status") String status);

    // 자동 연장 대상: 자동 연장이 켜져 있고 기간이 cutoff 이전에 끝나는(또는 이미 끝난) 구독
    @Select("""
        SELECT facility_id
          FROM subscriptions
         WHERE status = 'ACTIVE'
           AND current_period_end <= #{cutoff}
        """)
    List<Long> findFacilityIdsDueForRenewal(@Param("cutoff") LocalDateTime cutoff);

    @Insert("""
        INSERT INTO subscription_payments
            (facility_id, amount, status, pg_provider, pg_transaction_id, failure_reason, covered_until)
        VALUES
            (#{facilityId}, #{amount}, #{status}, #{pgProvider}, #{transactionId}, #{failureReason}, #{coveredUntil})
        """)
    int insertPayment(@Param("facilityId") Long facilityId,
                      @Param("amount") int amount,
                      @Param("status") String status,
                      @Param("pgProvider") String pgProvider,
                      @Param("transactionId") String transactionId,
                      @Param("failureReason") String failureReason,
                      @Param("coveredUntil") LocalDateTime coveredUntil);

    @Select("""
        SELECT id, amount, status,
               pg_provider AS pgProvider,
               DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS paidAt,
               DATE_FORMAT(DATE_SUB(covered_until, INTERVAL 1 DAY), '%Y-%m-%d') AS coveredUntil
          FROM subscription_payments
         WHERE facility_id = #{facilityId}
         ORDER BY id DESC
         LIMIT 24
        """)
    List<SubscriptionPaymentResponse> findPaymentsByFacilityId(@Param("facilityId") Long facilityId);
}
