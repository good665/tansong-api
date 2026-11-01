package kr.tansong.demo.facility.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 검색어 시도 이름 한글 ↔ 영문 */
class FacilityRegionAliasTest {

    @Test
    void 시도_이름은_반대_언어로_바꾼다() {
        assertThat(FacilityService.regionAlias("서울")).isEqualTo("Seoul");
        assertThat(FacilityService.regionAlias("서울특별시")).isEqualTo("Seoul");
        assertThat(FacilityService.regionAlias("SEOUL")).isEqualTo("서울");
        assertThat(FacilityService.regionAlias("경기도")).isEqualTo("Gyeonggi");
    }

    @Test
    void 시도_이름이_아니면_null() {
        assertThat(FacilityService.regionAlias("요양원")).isNull();
        assertThat(FacilityService.regionAlias("강남")).isNull();
    }
}
