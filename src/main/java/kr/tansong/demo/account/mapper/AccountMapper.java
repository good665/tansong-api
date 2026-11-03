package kr.tansong.demo.account.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 회원 탈퇴와 비밀번호 재설정 */
@Mapper
public interface AccountMapper {

    class AccountStatus {
        public boolean deleted;
        public Long passwordChangedAt; // 초 단위
    }

    class ResetCode {
        public String codeHash;
        public int attempts;
    }

    @Select("""
        SELECT deleted_at IS NOT NULL AS deleted, UNIX_TIMESTAMP(password_changed_at) AS passwordChangedAt
          FROM user_account_status WHERE user_id = #{userId}
        """)
    AccountStatus findAccountStatus(@Param("userId") Long userId);

    @Insert("""
        INSERT INTO user_account_status (user_id, deleted_at) VALUES (#{userId}, NOW())
        ON DUPLICATE KEY UPDATE deleted_at = NOW()
        """)
    int markDeleted(@Param("userId") Long userId);

    @Insert("""
        INSERT INTO user_account_status (user_id, password_changed_at) VALUES (#{userId}, NOW())
        ON DUPLICATE KEY UPDATE password_changed_at = NOW()
        """)
    int markPasswordChanged(@Param("userId") Long userId);

    @Select("SELECT EXISTS (SELECT 1 FROM user_social_accounts WHERE user_id = #{userId})")
    boolean hasSocialAccount(@Param("userId") Long userId);

    @Delete("DELETE FROM user_social_accounts WHERE user_id = #{userId}")
    int deleteSocialAccounts(@Param("userId") Long userId);

    // 고객 본인의 신청: 진행 중이면 취소하고, 자유 입력 내용은 지움
    @Update("""
        UPDATE consultations
           SET message = NULL, status = IF(status IN ('REQUESTED', 'CONFIRMED'), 'CANCELED', status)
         WHERE user_id = #{userId}
        """)
    int cancelMyConsultations(@Param("userId") Long userId);

    @Update("""
        UPDATE reservations
           SET request_message = NULL, status = IF(status IN ('REQUESTED', 'CONFIRMED'), 'CANCELED', status)
         WHERE user_id = #{userId}
        """)
    int cancelMyReservations(@Param("userId") Long userId);

    @Select("""
        SELECT document_key FROM facility_claims
         WHERE requester_user_id = #{userId} AND status = 'PENDING' AND document_key IS NOT NULL
        """)
    List<String> findPendingClaimDocuments(@Param("userId") Long userId);

    @Update("""
        UPDATE facility_claims
           SET status = 'REJECTED', reject_reason = #{reason}, decided_at = NOW(), document_key = NULL
         WHERE requester_user_id = #{userId} AND status = 'PENDING'
        """)
    int rejectPendingClaims(@Param("userId") Long userId, @Param("reason") String reason);

    @Update("""
        UPDATE subscriptions s
          JOIN facilities f ON f.id = s.facility_id
          JOIN organizations o ON o.id = f.organization_id
           SET s.status = 'CANCELED'
         WHERE o.owner_user_id = #{userId} AND s.status = 'ACTIVE'
        """)
    int cancelOwnedSubscriptions(@Param("userId") Long userId);

    @Delete("""
        DELETE b FROM billing_cards b
          JOIN facilities f ON f.id = b.facility_id
          JOIN organizations o ON o.id = f.organization_id
         WHERE o.owner_user_id = #{userId}
        """)
    int deleteOwnedBillingCards(@Param("userId") Long userId);

    @Update("""
        UPDATE consultations c
          JOIN facilities f ON f.id = c.facility_id
          JOIN organizations o ON o.id = f.organization_id
           SET c.status = 'CANCELED'
         WHERE o.owner_user_id = #{userId} AND c.status IN ('REQUESTED', 'CONFIRMED')
        """)
    int cancelOwnedFacilityConsultations(@Param("userId") Long userId);

    @Update("""
        UPDATE reservations r
          JOIN facilities f ON f.id = r.facility_id
          JOIN organizations o ON o.id = f.organization_id
           SET r.status = 'CANCELED'
         WHERE o.owner_user_id = #{userId} AND r.status IN ('REQUESTED', 'CONFIRMED')
        """)
    int cancelOwnedFacilityReservations(@Param("userId") Long userId);

    @Update("""
        UPDATE facilities f
          JOIN organizations o ON o.id = f.organization_id
           SET f.status = 'INACTIVE'
         WHERE o.owner_user_id = #{userId}
        """)
    int deactivateOwnedFacilities(@Param("userId") Long userId);

    @Update("UPDATE organizations SET owner_user_id = NULL, status = 'INACTIVE' WHERE owner_user_id = #{userId}")
    int releaseOrganizations(@Param("userId") Long userId);

    @Update("""
        UPDATE users
           SET name = '탈퇴한 사용자', email = #{email}, phone = NULL, password_hash = #{passwordHash},
               failed_login_attempts = 0, locked_until = NULL
         WHERE id = #{userId}
        """)
    int anonymizeUser(@Param("userId") Long userId, @Param("email") String email, @Param("passwordHash") String passwordHash);

    @Insert("""
        INSERT INTO password_reset_codes (user_id, code_hash, attempts, expires_at, created_at)
        VALUES (#{userId}, #{codeHash}, 0, NOW() + INTERVAL #{seconds} SECOND, NOW())
        ON DUPLICATE KEY UPDATE code_hash = #{codeHash}, attempts = 0,
                                expires_at = NOW() + INTERVAL #{seconds} SECOND, created_at = NOW()
        """)
    int saveResetCode(@Param("userId") Long userId, @Param("codeHash") String codeHash, @Param("seconds") int seconds);

    @Select("""
        SELECT EXISTS (SELECT 1 FROM password_reset_codes
                        WHERE user_id = #{userId} AND created_at > NOW() - INTERVAL #{seconds} SECOND)
        """)
    boolean hasRecentResetCode(@Param("userId") Long userId, @Param("seconds") int seconds);

    @Select("""
        SELECT code_hash AS codeHash, attempts
          FROM password_reset_codes WHERE user_id = #{userId} AND expires_at > NOW()
        """)
    ResetCode findValidResetCode(@Param("userId") Long userId);

    @Update("UPDATE password_reset_codes SET attempts = attempts + 1 WHERE user_id = #{userId}")
    int incrementResetAttempts(@Param("userId") Long userId);

    @Delete("DELETE FROM password_reset_codes WHERE user_id = #{userId}")
    int deletePasswordResetCode(@Param("userId") Long userId);
}
