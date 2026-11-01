package kr.tansong.demo.verification.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 기관별 국세청 진위확인 결과 (테이블 정의는 resources/schema.sql, 기관당 한 행) */
@Mapper
public interface BusinessVerificationMapper {

    @Insert("""
        INSERT INTO business_verifications
            (organization_id, representative_name, business_start_date, status, nts_status_code, message, checked_at)
        VALUES
            (#{organizationId}, #{representativeName}, #{startDate}, #{status}, #{ntsStatusCode}, #{message}, NOW())
        ON DUPLICATE KEY UPDATE
            representative_name = #{representativeName},
            business_start_date = #{startDate},
            status = #{status},
            nts_status_code = #{ntsStatusCode},
            message = #{message},
            checked_at = NOW()
        """)
    int upsert(@Param("organizationId") Long organizationId,
               @Param("representativeName") String representativeName,
               @Param("startDate") LocalDate startDate,
               @Param("status") String status,
               @Param("ntsStatusCode") String ntsStatusCode,
               @Param("message") String message);

    // 관리자 재확인용: 가입 때 저장해 둔 입력값
    @Select("""
        SELECT o.business_registration_number AS businessNumber,
               bv.representative_name          AS representativeName,
               bv.business_start_date          AS startDate
          FROM organizations o
          JOIN business_verifications bv ON bv.organization_id = o.id
         WHERE o.id = #{organizationId}
        """)
    StoredInput findStoredInput(@Param("organizationId") Long organizationId);

    class StoredInput {
        private String businessNumber;
        private String representativeName;
        private LocalDate startDate;

        public String getBusinessNumber() {
            return businessNumber;
        }

        public void setBusinessNumber(String businessNumber) {
            this.businessNumber = businessNumber;
        }

        public String getRepresentativeName() {
            return representativeName;
        }

        public void setRepresentativeName(String representativeName) {
            this.representativeName = representativeName;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }
    }
}
