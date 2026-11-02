package kr.tansong.demo.facilitydetail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import kr.tansong.demo.common.ImageStorageService;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.mapper.BusinessFacilityMapper;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsRequest;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.FloorPlan;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.Price;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.Room;
import kr.tansong.demo.facilitydetail.mapper.FacilityDetailsMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FacilityDetailsServiceTest {

    private static final Long FACILITY = 10L;
    private static final Long OWNER = 1L;

    @Mock
    private BusinessFacilityMapper businessFacilityMapper;
    @Mock
    private FacilityDetailsMapper mapper;
    @Mock
    private ImageStorageService storage;

    private FacilityDetailsService service;
    private final MockMultipartFile file = new MockMultipartFile("file", "a.png", null, new byte[] {1});

    @BeforeEach
    void setUp() {
        service = new FacilityDetailsService(businessFacilityMapper, mapper, storage);
        when(businessFacilityMapper.findByIdForOwner(FACILITY, OWNER)).thenReturn(new FacilityResponse());
    }

    private static Price price(String deal, Long deposit, Long amount) {
        Price p = new Price();
        p.dealType = deal;
        p.deposit = deposit;
        p.amount = amount;
        return p;
    }

    private static Room room(String type, String area, Price... prices) {
        Room r = new Room();
        r.roomType = type;
        r.areaSqm = area == null ? null : new BigDecimal(area);
        r.prices = List.of(prices);
        return r;
    }

    private static FacilityDetailsRequest request(Room... rooms) {
        FacilityDetailsRequest request = new FacilityDetailsRequest();
        request.rooms = List.of(rooms);
        return request;
    }

    private void assertRejectedWithoutTouchingData(FacilityDetailsRequest request) {
        assertThatThrownBy(() -> service.replace(FACILITY, OWNER, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).deleteRooms(any());
    }

    @Test
    void 다른_사업자_시설은_모든_메서드가_NotFound() {
        Long other = 2L;

        assertThatThrownBy(() -> service.getForOwner(FACILITY, other)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.replace(FACILITY, other, request())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.addFloorPlan(FACILITY, other, file, null)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deleteFloorPlan(FACILITY, other, 5L)).isInstanceOf(NotFoundException.class);

        verify(mapper, never()).deleteRooms(any());
        verify(storage, never()).storeFloorPlan(any(), any());
    }

    @Test
    void 다른_시설의_도면도_id는_404이고_파일을_지우지_않음() {
        when(mapper.findFloorPlan(5L, FACILITY)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteFloorPlan(FACILITY, OWNER, 5L)).isInstanceOf(NotFoundException.class);
        verify(storage, never()).deleteQuietly(any());
    }

    @Test
    void 도면도_삭제시_파일도_삭제() {
        FloorPlan plan = new FloorPlan();
        plan.fileUrl = "/uploads/facilities/10/plans/a.png";
        when(mapper.findFloorPlan(5L, FACILITY)).thenReturn(plan);

        service.deleteFloorPlan(FACILITY, OWNER, 5L);

        verify(mapper).deleteFloorPlan(5L, FACILITY);
        verify(storage).deleteQuietly(plan.fileUrl);
    }

    @Test
    void 도면도_20개_초과_거절() {
        when(mapper.countFloorPlans(FACILITY)).thenReturn(20);

        assertThatThrownBy(() -> service.addFloorPlan(FACILITY, OWNER, file, "1층"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(storage, never()).storeFloorPlan(any(), any());
    }

    @Test
    void 도면도_층이름_30자_초과_거절() {
        assertThatThrownBy(() -> service.addFloorPlan(FACILITY, OWNER, file, "가".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(storage, never()).storeFloorPlan(any(), any());
    }

    @Test
    void 한_방에서_거래유형_중복_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16",
                price("SALE", null, 100L), price("SALE", null, 200L))));
    }

    @Test
    void 다른_방은_같은_거래유형_허용() {
        assertThatCode(() -> service.replace(FACILITY, OWNER, request(
                room("1인실", "16", price("SALE", null, 100L)),
                room("2인실", "26", price("SALE", null, 200L)))))
                .doesNotThrowAnyException();
    }

    @Test
    void 면적과_가격이_없는_방_허용() {
        Room empty = room("1인실", null);
        empty.prices = null;

        service.replace(FACILITY, OWNER, request(empty));

        verify(mapper).insertRoom(FACILITY, empty);
        verify(mapper, never()).insertPrice(any(), any());
    }

    @Test
    void MONTHLY_월세_없으면_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16", price("MONTHLY", 1000L, null))));
    }

    @Test
    void 보증금_0원_월세는_허용() {
        assertThatCode(() -> service.replace(FACILITY, OWNER, request(room("1인실", "16", price("MONTHLY", 0L, 100L)))))
                .doesNotThrowAnyException();
    }

    @Test
    void 음수_금액_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16", price("MONTHLY", -1L, 100L))));
    }

    @Test
    void SALE_보증금은_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16", price("SALE", 1L, 100L))));
    }

    @Test
    void JEONSE_보증금_없으면_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16", price("JEONSE", null, null))));
    }

    @Test
    void 금액_1조_초과_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16", price("SALE", null, 1_000_000_000_001L))));
    }

    @Test
    void 알수없는_거래유형_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "16", price("LEASE", null, 100L))));
    }

    @Test
    void 면적_0_거절() {
        assertRejectedWithoutTouchingData(request(room("1인실", "0")));
    }

    @Test
    void 방_이름_공백_거절() {
        assertRejectedWithoutTouchingData(request(room("  ", "16")));
    }

    @Test
    void 방_30개_초과_거절() {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            rooms.add(room("방" + i, "16"));
        }
        FacilityDetailsRequest request = new FacilityDetailsRequest();
        request.rooms = rooms;

        assertRejectedWithoutTouchingData(request);
    }

    @Test
    void 빈_목록이면_모두_지움() {
        service.replace(FACILITY, OWNER, request());

        verify(mapper).deleteRooms(FACILITY);
        verify(mapper, never()).insertRoom(any(), any());
    }

    @Test
    void 공개_조회는_도면도와_방을_함께_반환() {
        when(mapper.findFloorPlans(FACILITY)).thenReturn(List.of(new FloorPlan()));
        when(mapper.findRooms(FACILITY)).thenReturn(List.of(new Room()));

        var details = service.getPublic(FACILITY);

        assertThat(details.floorPlans).hasSize(1);
        assertThat(details.rooms).hasSize(1);
    }
}
