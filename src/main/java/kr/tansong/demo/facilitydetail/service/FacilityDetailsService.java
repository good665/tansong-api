package kr.tansong.demo.facilitydetail.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.common.ImageStorageService;
import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.facility.mapper.BusinessFacilityMapper;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsRequest;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.FloorPlan;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.Price;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.Room;
import kr.tansong.demo.facilitydetail.mapper.FacilityDetailsMapper;

/** 사업자는 본인 소유 시설만 등록·수정. 공개 여부(광고 유효)는 FacilityService 에서 판단 */
@Service
public class FacilityDetailsService {

    private static final int MAX_ROOMS = 30;
    private static final int MAX_FLOOR_PLANS = 20;
    private static final int MAX_ROOM_TYPE_LENGTH = 50;
    private static final int MAX_LABEL_LENGTH = 30;
    private static final long MAX_PRICE = 1_000_000_000_000L;
    private static final BigDecimal MAX_AREA = new BigDecimal("9999.99");

    private final BusinessFacilityMapper businessFacilityMapper;
    private final FacilityDetailsMapper mapper;
    private final ImageStorageService storage;

    public FacilityDetailsService(BusinessFacilityMapper businessFacilityMapper,
                                  FacilityDetailsMapper mapper,
                                  ImageStorageService storage) {
        this.businessFacilityMapper = businessFacilityMapper;
        this.mapper = mapper;
        this.storage = storage;
    }

    public FacilityDetailsResponse getPublic(Long facilityId) {
        FacilityDetailsResponse details = new FacilityDetailsResponse();
        details.floorPlans = mapper.findFloorPlans(facilityId);
        details.rooms = mapper.findRooms(facilityId);
        return details;
    }

    public FacilityDetailsResponse getForOwner(Long facilityId, Long ownerUserId) {
        requireOwned(facilityId, ownerUserId);
        return getPublic(facilityId);
    }

    @Transactional
    public FacilityDetailsResponse replace(Long facilityId, Long ownerUserId, FacilityDetailsRequest request) {
        requireOwned(facilityId, ownerUserId);

        List<Room> rooms = request == null || request.rooms == null ? List.of() : request.rooms;
        validateRooms(rooms);

        mapper.deleteRooms(facilityId);
        for (Room room : rooms) {
            room.roomType = room.roomType.trim();
            mapper.insertRoom(facilityId, room);
            if (room.prices != null) {
                for (Price price : room.prices) {
                    mapper.insertPrice(room.id, price);
                }
            }
        }
        return getPublic(facilityId);
    }

    @Transactional
    public FloorPlan addFloorPlan(Long facilityId, Long ownerUserId, MultipartFile file, String label) {
        requireOwned(facilityId, ownerUserId);

        String trimmed = label == null || label.isBlank() ? null : label.trim();
        if (trimmed != null && trimmed.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("층 이름은 " + MAX_LABEL_LENGTH + "자 이하로 입력해 주세요.");
        }
        if (mapper.countFloorPlans(facilityId) >= MAX_FLOOR_PLANS) {
            throw new IllegalArgumentException("도면도는 " + MAX_FLOOR_PLANS + "개까지 등록할 수 있습니다.");
        }

        ImageStorageService.StoredFloorPlan stored = storage.storeFloorPlan(facilityId, file);

        FloorPlan plan = new FloorPlan();
        plan.fileUrl = stored.url();
        plan.fileType = stored.fileType();
        plan.label = trimmed;
        mapper.insertFloorPlan(facilityId, plan);
        return plan;
    }

    @Transactional
    public void deleteFloorPlan(Long facilityId, Long ownerUserId, Long planId) {
        requireOwned(facilityId, ownerUserId);

        FloorPlan plan = mapper.findFloorPlan(planId, facilityId);
        if (plan == null) {
            throw new NotFoundException("도면도를 찾을 수 없습니다.");
        }
        mapper.deleteFloorPlan(planId, facilityId);
        storage.deleteQuietly(plan.fileUrl);
    }

    private void requireOwned(Long facilityId, Long ownerUserId) {
        if (facilityId == null || facilityId <= 0
                || businessFacilityMapper.findByIdForOwner(facilityId, ownerUserId) == null) {
            throw new NotFoundException("존재하지 않는 시설입니다.");
        }
    }

    private static void validateRooms(List<Room> rooms) {
        if (rooms.size() > MAX_ROOMS) {
            throw new IllegalArgumentException("방 종류는 " + MAX_ROOMS + "개까지 등록할 수 있습니다.");
        }
        for (Room room : rooms) {
            if (room == null || room.roomType == null || room.roomType.isBlank()) {
                throw new IllegalArgumentException("방 종류를 입력해 주세요.");
            }
            if (room.roomType.trim().length() > MAX_ROOM_TYPE_LENGTH) {
                throw new IllegalArgumentException("방 종류는 " + MAX_ROOM_TYPE_LENGTH + "자 이하로 입력해 주세요.");
            }
            if (room.areaSqm != null && (room.areaSqm.signum() <= 0 || room.areaSqm.compareTo(MAX_AREA) > 0)) {
                throw new IllegalArgumentException("방 크기는 0보다 크고 " + MAX_AREA + "㎡ 이하로 입력해 주세요.");
            }
            validatePrices(room);
        }
    }

    private static void validatePrices(Room room) {
        if (room.prices == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Price price : room.prices) {
            if (price == null || price.dealType == null) {
                throw new IllegalArgumentException("거래 유형을 선택해 주세요.");
            }
            if (!seen.add(price.dealType)) {
                throw new IllegalArgumentException(room.roomType + ": 거래 유형이 중복되었습니다.");
            }
            boolean valid = switch (price.dealType) {
                case "MONTHLY" -> inRange(price.deposit, 0) && inRange(price.amount, 1);
                case "JEONSE" -> inRange(price.deposit, 1) && price.amount == null;
                case "SALE" -> price.deposit == null && inRange(price.amount, 1);
                default -> false;
            };
            if (!valid) {
                throw new IllegalArgumentException(room.roomType + ": 가격을 올바르게 입력해 주세요.");
            }
        }
    }

    private static boolean inRange(Long value, long min) {
        return value != null && value >= min && value <= MAX_PRICE;
    }
}
