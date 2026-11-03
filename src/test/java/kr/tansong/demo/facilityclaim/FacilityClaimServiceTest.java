package kr.tansong.demo.facilityclaim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import kr.tansong.demo.common.ConflictException;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.mapper.OrganizationMapper;
import kr.tansong.demo.facilityclaim.dto.FacilityClaimResponse;
import kr.tansong.demo.facilityclaim.mapper.FacilityClaimMapper;

/** 다른 사람의 시설을 가로채지 못하도록 소유권 이전은 관리자 승인으로만, 서류는 비공개로 처리 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FacilityClaimServiceTest {

    private static final Long OWNER = 1L;
    private static final Long ADMIN = 99L;
    private static final Long FACILITY = 10L;
    private static final Long ORG = 5L;

    @Mock
    private FacilityClaimMapper mapper;
    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private ClaimDocumentStorage storage;

    private FacilityClaimService service;
    private final MockMultipartFile file = new MockMultipartFile("file", "a.pdf", null, new byte[] {1});

    @BeforeEach
    void setUp() {
        service = new FacilityClaimService(mapper, organizationMapper, storage,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                "test-secret-test-secret-test-secret-1234");
        when(organizationMapper.countOwnedByUser(ORG, OWNER)).thenReturn(1);
        when(mapper.lockClaimableFacility(FACILITY)).thenReturn(FACILITY);
        when(storage.store(any())).thenReturn("doc.pdf");
    }

    private void pendingClaim(Long id, String key) {
        FacilityClaimResponse claim = new FacilityClaimResponse();
        claim.id = id;
        claim.facilityId = FACILITY;
        claim.organizationId = ORG;
        claim.status = "PENDING";
        claim.documentKey = key;
        when(mapper.lockClaim(id)).thenReturn(claim);
    }

    @Test
    void 신청하면_서류를_저장하고_대기_상태로_기록한다() {
        service.create(OWNER, FACILITY, ORG, file, " 저희 시설입니다 ");

        verify(storage).store(file);
        verify(mapper).insert(FACILITY, ORG, OWNER, "doc.pdf", "저희 시설입니다");
    }

    @Test
    void 내_기관이_아니면_서류를_저장하지_않고_404() {
        when(organizationMapper.countOwnedByUser(ORG, OWNER)).thenReturn(0);

        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, file, null)).isInstanceOf(NotFoundException.class);
        verify(storage, never()).store(any());
    }

    @Test
    void 공공데이터_시설이_아니거나_이미_이전됐으면_신청할_수_없다() {
        when(mapper.lockClaimableFacility(FACILITY)).thenReturn(null);

        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, file, null)).isInstanceOf(ConflictException.class);
        verify(mapper, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void 같은_시설에_대기중인_신청이_있으면_중복_신청할_수_없다() {
        when(mapper.countPending(FACILITY, OWNER)).thenReturn(1);

        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, file, null)).isInstanceOf(ConflictException.class);
        verify(storage, never()).store(any());
    }

    @Test
    void 대기중인_신청은_20건까지만_받는다() {
        when(mapper.countPendingByRequester(OWNER)).thenReturn(20);

        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, file, null)).isInstanceOf(ConflictException.class);
        verify(storage, never()).store(any());
    }

    @Test
    void 서류가_없거나_내용이_너무_길면_거절한다() {
        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, file, "가".repeat(501))).isInstanceOf(IllegalArgumentException.class);
        verify(storage, never()).store(any());
    }

    @Test
    void 기록에_실패하면_저장한_서류를_지운다() {
        doThrow(new IllegalStateException("db")).when(mapper).insert(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.create(OWNER, FACILITY, ORG, file, null)).isInstanceOf(IllegalStateException.class);
        verify(storage).delete("doc.pdf");
    }

    @Test
    void 승인하면_소유권을_옮기고_공공데이터_표시를_끄고_서류를_지운다() {
        pendingClaim(7L, "doc.pdf");
        FacilityClaimResponse other = new FacilityClaimResponse();
        other.id = 8L;
        other.documentKey = "other.pdf";
        when(mapper.findOtherPending(FACILITY, 7L)).thenReturn(List.of(other));

        service.approve(7L, ADMIN);

        verify(mapper).transferFacility(FACILITY, ORG);
        verify(mapper).markSourceClaimed(FACILITY);
        verify(mapper).stripPublicDataIntro(FACILITY);
        verify(mapper).markApproved(7L, ADMIN);
        verify(mapper).markRejected(eq(8L), eq(ADMIN), anyString());
        verify(storage).delete("doc.pdf");
        verify(storage).delete("other.pdf");
    }

    @Test
    void 이미_처리된_신청은_다시_승인할_수_없다() {
        FacilityClaimResponse done = new FacilityClaimResponse();
        done.status = "REJECTED";
        when(mapper.lockClaim(7L)).thenReturn(done);

        assertThatThrownBy(() -> service.approve(7L, ADMIN)).isInstanceOf(ConflictException.class);
        verify(mapper, never()).transferFacility(anyLong(), anyLong());
    }

    @Test
    void 없는_신청은_404() {
        when(mapper.lockClaim(7L)).thenReturn(null);

        assertThatThrownBy(() -> service.approve(7L, ADMIN)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.reject(7L, ADMIN, "사유")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 그사이_시설이_이전되거나_사라졌으면_승인하지_않고_서류도_남긴다() {
        pendingClaim(7L, "doc.pdf");
        when(mapper.lockClaimableFacility(FACILITY)).thenReturn(null);

        assertThatThrownBy(() -> service.approve(7L, ADMIN)).isInstanceOf(ConflictException.class);
        verify(mapper, never()).transferFacility(anyLong(), anyLong());
        verify(storage, never()).delete(any());
    }

    @Test
    void 거절은_사유가_필요하고_서류를_지운다() {
        pendingClaim(7L, "doc.pdf");

        assertThatThrownBy(() -> service.reject(7L, ADMIN, "  ")).isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).markRejected(anyLong(), anyLong(), anyString());

        service.reject(7L, ADMIN, " 서류가 흐립니다 ");

        verify(mapper).markRejected(7L, ADMIN, "서류가 흐립니다");
        verify(storage).delete("doc.pdf");
        verify(mapper, never()).transferFacility(anyLong(), anyLong());
    }

    private String[] linkParts(Long id) {
        URI link = URI.create(service.documentLink(id));
        String[] query = link.getQuery().split("&");
        return new String[] {query[0].split("=")[1], query[1].split("=")[1]};
    }

    @Test
    void 서류_링크는_서명이_맞을_때만_열린다() {
        FacilityClaimResponse claim = new FacilityClaimResponse();
        claim.documentKey = "doc.pdf";
        when(mapper.findDocumentClaim(7L)).thenReturn(claim);
        when(mapper.findDocumentClaim(8L)).thenReturn(claim);
        String[] link = linkParts(7L);
        long expires = Long.parseLong(link[0]);

        assertThat(service.documentKey(7L, expires, link[1])).isEqualTo("doc.pdf");

        assertThatThrownBy(() -> service.documentKey(7L, expires, "forged")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.documentKey(7L, expires + 1, link[1])).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.documentKey(8L, expires, link[1])).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 만료된_링크와_서류가_지워진_신청은_열리지_않는다() {
        when(mapper.findDocumentClaim(7L)).thenReturn(new FacilityClaimResponse());
        String[] link = linkParts(7L);

        assertThatThrownBy(() -> service.documentKey(7L, Long.parseLong(link[0]), link[1])).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.documentKey(7L, 1000L, link[1])).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 검색어가_두_글자_미만이면_조회하지_않고_특수문자는_이스케이프한다() {
        assertThat(service.searchClaimable(OWNER, "가", 0, 10).getItems()).isEmpty();
        verify(mapper, never()).findClaimable(any(), any(), anyInt(), anyInt());

        service.searchClaimable(OWNER, " 100%_요양 ", 0, 10);

        verify(mapper).findClaimable("100\\%\\_요양", OWNER, 10, 0);
    }
}
