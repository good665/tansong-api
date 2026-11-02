package kr.tansong.demo.facilitydetail.dto;

import java.math.BigDecimal;
import java.util.List;

/** 광고 시설의 추가 정보: 도면도, 방 종류(면적, 거래 유형별 가격) */
public class FacilityDetailsResponse {

    public List<FloorPlan> floorPlans;
    public List<Room> rooms;

    public static class FloorPlan {
        public Long id;
        public String fileUrl;
        public String fileType; // IMAGE 또는 PDF
        public String label;    // 층 이름 등 (선택)
    }

    public static class Room {
        public Long id;
        public String roomType;
        public BigDecimal areaSqm; // 비어 있을 수 있음
        public List<Price> prices;
    }

    /** MONTHLY: deposit(보증금)+amount(월세), JEONSE: deposit(전세금), SALE: amount(매매가). 단위 원 */
    public static class Price {
        public String dealType;
        public Long deposit;
        public Long amount;
    }
}
