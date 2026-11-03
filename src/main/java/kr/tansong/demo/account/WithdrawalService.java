package kr.tansong.demo.account;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.auth.service.PasswordVerifier;
import kr.tansong.demo.account.mapper.AccountMapper;
import kr.tansong.demo.common.UnauthorizedException;
import kr.tansong.demo.facilityclaim.ClaimDocumentStorage;
import kr.tansong.demo.user.domain.User;
import kr.tansong.demo.user.mapper.UserMapper;

/**
 * 회원 탈퇴. 다른 기록(결제, 처리 이력)이 사용자를 참조하므로 행을 지우지 않고 개인정보를 익명화합니다.
 * 사업자는 자동 결제가 끊기고 시설이 비공개로 바뀌며, 같은 이메일로 다시 가입할 수 있습니다.
 */
@Service
public class WithdrawalService {

    private static final String CLAIM_REJECT_REASON = "신청자가 탈퇴했습니다.";

    private final AccountMapper mapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordVerifier passwordVerifier;
    private final ClaimDocumentStorage documents;
    private final TransactionTemplate tx;

    public WithdrawalService(AccountMapper mapper, UserMapper userMapper, PasswordEncoder passwordEncoder,
                             PasswordVerifier passwordVerifier,
                             ClaimDocumentStorage documents, TransactionTemplate tx) {
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordVerifier = passwordVerifier;
        this.documents = documents;
        this.tx = tx;
    }

    /** 소셜 계정은 비밀번호가 없어 화면에서 입력을 받지 않음 */
    public boolean isSocialOnly(Long userId) {
        return mapper.hasSocialAccount(userId);
    }

    public void withdraw(Long userId, String password) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new UnauthorizedException("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
        }
        // 마지막 관리자가 사라지면 복구할 방법이 없음
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("관리자 계정은 앱에서 탈퇴할 수 없습니다.");
        }
        boolean social = mapper.hasSocialAccount(userId);
        if (!social && !passwordVerifier.verify(user, password)) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());

        List<String> claimDocuments = tx.execute(status -> {
            // 소유 관계로 대상을 찾으므로 소유를 끊는 일은 마지막에 함
            mapper.cancelOwnedSubscriptions(userId);
            mapper.deleteOwnedBillingCards(userId);
            mapper.cancelOwnedFacilityConsultations(userId);
            mapper.cancelOwnedFacilityReservations(userId);
            mapper.deactivateOwnedFacilities(userId);
            mapper.releaseOrganizations(userId);

            List<String> keys = mapper.findPendingClaimDocuments(userId);
            mapper.rejectPendingClaims(userId, CLAIM_REJECT_REASON);

            mapper.cancelMyConsultations(userId);
            mapper.cancelMyReservations(userId);
            mapper.deleteSocialAccounts(userId);
            mapper.deletePasswordResetCode(userId);
            mapper.anonymizeUser(userId, "deleted_" + userId + "@deleted.invalid", unusablePassword);
            mapper.markDeleted(userId);
            return keys;
        });

        // 파일 삭제는 되돌릴 수 없으므로 DB 변경이 확정된 뒤에 함
        claimDocuments.forEach(documents::delete);
    }
}
