package kr.tansong.demo.facilityclaim;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.common.Paging;
import kr.tansong.demo.facility.mapper.OrganizationMapper;
import kr.tansong.demo.facilityclaim.dto.ClaimableFacilityResponse;
import kr.tansong.demo.facilityclaim.dto.FacilityClaimResponse;
import kr.tansong.demo.facilityclaim.mapper.FacilityClaimMapper;

/**
 * 사업자가 공공데이터 시설의 소유권을 신청하고 관리자가 승인합니다.
 * 자동 승인은 없습니다. 증빙 서류는 비공개로 보관하고 승인·거절되면 지웁니다.
 */
@Service
public class FacilityClaimService {

    private static final int MIN_KEYWORD = 2;
    private static final int MAX_KEYWORD = 50;
    private static final int MAX_MESSAGE = 500;
    private static final int MAX_REASON = 500;
    private static final int MAX_PENDING = 20;
    private static final long LINK_SECONDS = 300;
    private static final String OTHER_APPROVED = "같은 시설의 다른 신청이 승인되었습니다.";

    private final FacilityClaimMapper mapper;
    private final OrganizationMapper organizationMapper;
    private final ClaimDocumentStorage storage;
    private final TransactionTemplate tx;
    private final byte[] linkSecret;

    public FacilityClaimService(FacilityClaimMapper mapper, OrganizationMapper organizationMapper,
                                ClaimDocumentStorage storage, TransactionTemplate tx,
                                @Value("${app.jwt.secret}") String secret) {
        this.mapper = mapper;
        this.organizationMapper = organizationMapper;
        this.storage = storage;
        this.tx = tx;
        this.linkSecret = ("facility-claim-document:" + secret).getBytes(StandardCharsets.UTF_8);
    }

    public PageResponse<ClaimableFacilityResponse> searchClaimable(Long ownerUserId, String keyword, int page, int size) {
        int p = Paging.page(page);
        int s = Paging.size(size);
        String k = keyword == null ? "" : keyword.trim();
        if (k.length() < MIN_KEYWORD) {
            return new PageResponse<>(List.of(), p, s, 0);
        }
        if (k.length() > MAX_KEYWORD) {
            k = k.substring(0, MAX_KEYWORD);
        }
        String escaped = k.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return new PageResponse<>(mapper.findClaimable(escaped, ownerUserId, s, p * s), p, s, mapper.countClaimable(escaped));
    }

    public void create(Long ownerUserId, Long facilityId, Long organizationId, MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("증빙 서류를 첨부해 주세요.");
        }
        String trimmed = message == null || message.isBlank() ? null : message.trim();
        if (trimmed != null && trimmed.length() > MAX_MESSAGE) {
            throw new IllegalArgumentException("요청 내용은 " + MAX_MESSAGE + "자 이하로 입력해 주세요.");
        }
        if (organizationId == null || organizationMapper.countOwnedByUser(organizationId, ownerUserId) == 0) {
            throw new NotFoundException("운영 기관을 찾을 수 없습니다.");
        }

        String[] stored = new String[1];
        try {
            tx.executeWithoutResult(status -> {
                if (mapper.lockClaimableFacility(facilityId) == null) {
                    throw new ConflictException("소유권을 신청할 수 없는 시설입니다.");
                }
                if (mapper.countPending(facilityId, ownerUserId) > 0) {
                    throw new ConflictException("이 시설은 이미 신청하여 검토 중입니다.");
                }
                if (mapper.countPendingByRequester(ownerUserId) >= MAX_PENDING) {
                    throw new ConflictException("검토 중인 신청이 너무 많습니다. 결과가 나온 뒤 다시 신청해 주세요.");
                }
                stored[0] = storage.store(file);
                mapper.insert(facilityId, organizationId, ownerUserId, stored[0], trimmed);
            });
        } catch (RuntimeException e) {
            storage.delete(stored[0]);
            throw e;
        }
    }

    public List<FacilityClaimResponse> findMine(Long ownerUserId) {
        return mapper.findMine(ownerUserId);
    }

    public PageResponse<FacilityClaimResponse> listForAdmin(String status, int page, int size) {
        int p = Paging.page(page);
        int s = Paging.size(size);
        String filter = status == null || status.isBlank() || "ALL".equals(status) ? null : status.trim();
        return new PageResponse<>(mapper.findAdmin(filter, s, p * s), p, s, mapper.countAdmin(filter));
    }

    public void approve(Long claimId, Long adminUserId) {
        // 파일 삭제는 되돌릴 수 없으므로 DB 변경이 확정된 뒤에 함
        List<String> documents = tx.execute(status -> {
            FacilityClaimResponse claim = lockPending(claimId);
            if (mapper.lockClaimableFacility(claim.facilityId) == null) {
                throw new ConflictException("이미 소유권이 이전되었거나 운영하지 않는 시설입니다.");
            }

            mapper.transferFacility(claim.facilityId, claim.organizationId);
            mapper.markSourceClaimed(claim.facilityId);
            mapper.stripPublicDataIntro(claim.facilityId);
            mapper.markApproved(claimId, adminUserId);

            List<String> keys = new ArrayList<>();
            keys.add(claim.documentKey);
            for (FacilityClaimResponse other : mapper.findOtherPending(claim.facilityId, claimId)) {
                mapper.markRejected(other.id, adminUserId, OTHER_APPROVED);
                keys.add(other.documentKey);
            }
            return keys;
        });
        documents.forEach(storage::delete);
    }

    public void reject(Long claimId, Long adminUserId, String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_REASON) {
            throw new IllegalArgumentException("거절 사유를 1~" + MAX_REASON + "자로 입력해 주세요.");
        }
        String document = tx.execute(status -> {
            FacilityClaimResponse claim = lockPending(claimId);
            mapper.markRejected(claimId, adminUserId, trimmed);
            return claim.documentKey;
        });
        storage.delete(document);
    }

    private FacilityClaimResponse lockPending(Long claimId) {
        FacilityClaimResponse claim = mapper.lockClaim(claimId);
        if (claim == null) {
            throw new NotFoundException("소유권 신청을 찾을 수 없습니다.");
        }
        if (!"PENDING".equals(claim.status)) {
            throw new ConflictException("이미 처리된 신청입니다.");
        }
        return claim;
    }

    /** 관리자가 브라우저로 서류를 열 수 있는 5분짜리 서명 주소 */
    public String documentLink(Long claimId) {
        long expires = Instant.now().getEpochSecond() + LINK_SECONDS;
        return "/api/facility-claim-documents/" + claimId + "?expires=" + expires + "&signature=" + sign(claimId, expires);
    }

    /** 서명과 만료를 확인하고 저장 위치를 돌려줌 */
    public String documentKey(Long claimId, long expires, String signature) {
        boolean valid = signature != null && expires >= Instant.now().getEpochSecond()
                && MessageDigest.isEqual(sign(claimId, expires).getBytes(StandardCharsets.US_ASCII),
                        signature.getBytes(StandardCharsets.US_ASCII));
        FacilityClaimResponse claim = valid ? mapper.findDocumentClaim(claimId) : null;
        if (claim == null || claim.documentKey == null) {
            throw new NotFoundException("서류를 찾을 수 없습니다.");
        }
        return claim.documentKey;
    }

    private String sign(Long claimId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(linkSecret, "HmacSHA256"));
            byte[] bytes = mac.doFinal((claimId + ":" + expires).getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
