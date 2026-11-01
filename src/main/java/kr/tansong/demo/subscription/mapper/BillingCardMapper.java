package kr.tansong.demo.subscription.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.tansong.demo.subscription.domain.BillingCard;

/** 시설별 결제 카드(토스 빌링키). 등록 중인 customerKey 는 pending_customer_key 에 따로 두어,
 *  등록을 중간에 포기해도 기존 카드(customer_key + billing_key 쌍)로 자동 연장이 계속되게 합니다. */
@Mapper
public interface BillingCardMapper {

    @Select("""
        SELECT facility_id AS facilityId, customer_key AS customerKey,
               billing_key AS billingKey, card_summary AS cardSummary
          FROM billing_cards
         WHERE facility_id = #{facilityId}
           AND billing_key IS NOT NULL
        """)
    BillingCard findRegistered(@Param("facilityId") Long facilityId);

    @Insert("""
        INSERT INTO billing_cards (facility_id, pending_customer_key)
        VALUES (#{facilityId}, #{customerKey})
        ON DUPLICATE KEY UPDATE pending_customer_key = #{customerKey}
        """)
    int startRegistration(@Param("facilityId") Long facilityId, @Param("customerKey") String customerKey);

    @Select("SELECT facility_id FROM billing_cards WHERE pending_customer_key = #{customerKey} FOR UPDATE")
    Long findFacilityIdByPendingKey(@Param("customerKey") String customerKey);

    @Update("""
        UPDATE billing_cards
           SET customer_key = pending_customer_key,
               pending_customer_key = NULL,
               billing_key = #{billingKey},
               card_summary = #{cardSummary},
               registered_at = NOW()
         WHERE pending_customer_key = #{customerKey}
        """)
    int completeRegistration(@Param("customerKey") String customerKey,
                             @Param("billingKey") String billingKey,
                             @Param("cardSummary") String cardSummary);
}
