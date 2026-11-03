package kr.tansong.demo.facilityclaim;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.common.PageResponse;
import kr.tansong.demo.facilityclaim.dto.ClaimableFacilityResponse;
import kr.tansong.demo.facilityclaim.dto.FacilityClaimResponse;

/** 시설 소유권 신청 API. 사업자·관리자·서류 열람의 권한이 달라 경로를 나눔 (SecurityConfig 참고) */
public final class FacilityClaimControllers {

    private FacilityClaimControllers() {}

    @RestController
    @RequestMapping("/api/business/facility-claims")
    public static class Business {

        private final FacilityClaimService service;

        public Business(FacilityClaimService service) {
            this.service = service;
        }

        @GetMapping("/claimable")
        public PageResponse<ClaimableFacilityResponse> claimable(
                @AuthenticationPrincipal Long ownerUserId,
                @RequestParam(required = false) String keyword,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            return service.searchClaimable(ownerUserId, keyword, page, size);
        }

        @GetMapping
        public List<FacilityClaimResponse> mine(@AuthenticationPrincipal Long ownerUserId) {
            return service.findMine(ownerUserId);
        }

        @PostMapping
        public ResponseEntity<?> create(
                @AuthenticationPrincipal Long ownerUserId,
                @RequestParam Long facilityId,
                @RequestParam Long organizationId,
                @RequestParam(value = "file", required = false) MultipartFile file,
                @RequestParam(required = false) String message) {

            service.create(ownerUserId, facilityId, organizationId, file, message);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "소유권 신청이 접수되었습니다."));
        }
    }

    @RestController
    @RequestMapping("/api/admin/facility-claims")
    public static class Admin {

        private final FacilityClaimService service;

        public Admin(FacilityClaimService service) {
            this.service = service;
        }

        @GetMapping
        public PageResponse<FacilityClaimResponse> list(
                @RequestParam(required = false) String status,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            return service.listForAdmin(status, page, size);
        }

        @PostMapping("/{id}/approve")
        public ResponseEntity<?> approve(@AuthenticationPrincipal Long adminUserId, @PathVariable Long id) {
            service.approve(id, adminUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "소유권을 이전했습니다."));
        }

        @PostMapping("/{id}/reject")
        public ResponseEntity<?> reject(@AuthenticationPrincipal Long adminUserId, @PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
            service.reject(id, adminUserId, body == null ? null : body.get("reason"));
            return ResponseEntity.ok(Map.of("success", true, "message", "신청을 거절했습니다."));
        }

        @PostMapping("/{id}/document-link")
        public Map<String, String> documentLink(@PathVariable Long id) {
            return Map.of("url", service.documentLink(id));
        }
    }

    // 관리자 앱이 외부 브라우저로 여는 주소라 헤더 대신 서명으로 확인
    @RestController
    @RequestMapping("/api/facility-claim-documents")
    public static class Documents {

        private final FacilityClaimService service;
        private final ClaimDocumentStorage storage;

        public Documents(FacilityClaimService service, ClaimDocumentStorage storage) {
            this.service = service;
            this.storage = storage;
        }

        @GetMapping("/{id}")
        public ResponseEntity<Resource> open(@PathVariable Long id, @RequestParam long expires,
                                             @RequestParam String signature) {
            String key = service.documentKey(id, expires, signature);
            Path path = storage.path(key);
            return ResponseEntity.ok()
                    .contentType(storage.mediaType(key))
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(new FileSystemResource(path));
        }
    }
}
