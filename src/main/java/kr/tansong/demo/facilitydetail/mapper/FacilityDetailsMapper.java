package kr.tansong.demo.facilitydetail.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.FloorPlan;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.Price;
import kr.tansong.demo.facilitydetail.dto.FacilityDetailsResponse.Room;

/** 도면도(facility_floor_plans)와 방 종류·가격(facility_rooms, facility_room_prices) */
@Mapper
public interface FacilityDetailsMapper {

    @Select("""
        SELECT id, file_url AS fileUrl, file_type AS fileType, label
          FROM facility_floor_plans
         WHERE facility_id = #{facilityId}
         ORDER BY id
        """)
    List<FloorPlan> findFloorPlans(@Param("facilityId") Long facilityId);

    @Select("""
        SELECT id, file_url AS fileUrl, file_type AS fileType, label
          FROM facility_floor_plans
         WHERE id = #{id} AND facility_id = #{facilityId}
        """)
    FloorPlan findFloorPlan(@Param("id") Long id, @Param("facilityId") Long facilityId);

    @Select("SELECT COUNT(*) FROM facility_floor_plans WHERE facility_id = #{facilityId}")
    int countFloorPlans(@Param("facilityId") Long facilityId);

    @Insert("""
        INSERT INTO facility_floor_plans (facility_id, file_url, file_type, label)
        VALUES (#{facilityId}, #{plan.fileUrl}, #{plan.fileType}, #{plan.label})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "plan.id")
    int insertFloorPlan(@Param("facilityId") Long facilityId, @Param("plan") FloorPlan plan);

    @Delete("DELETE FROM facility_floor_plans WHERE id = #{id} AND facility_id = #{facilityId}")
    int deleteFloorPlan(@Param("id") Long id, @Param("facilityId") Long facilityId);

    @Select("""
        SELECT id, room_type AS roomType, area_sqm AS areaSqm
          FROM facility_rooms
         WHERE facility_id = #{facilityId}
         ORDER BY id
        """)
    @Results({
        @Result(property = "id", column = "id", id = true),
        @Result(property = "prices", column = "id", many = @Many(select = "findPrices"))
    })
    List<Room> findRooms(@Param("facilityId") Long facilityId);

    @Select("SELECT deal_type AS dealType, deposit, amount FROM facility_room_prices WHERE room_id = #{roomId}")
    List<Price> findPrices(@Param("roomId") Long roomId);

    // 가격은 ON DELETE CASCADE 로 함께 지워짐
    @Delete("DELETE FROM facility_rooms WHERE facility_id = #{facilityId}")
    int deleteRooms(@Param("facilityId") Long facilityId);

    @Insert("""
        INSERT INTO facility_rooms (facility_id, room_type, area_sqm)
        VALUES (#{facilityId}, #{room.roomType}, #{room.areaSqm})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "room.id")
    int insertRoom(@Param("facilityId") Long facilityId, @Param("room") Room room);

    @Insert("""
        INSERT INTO facility_room_prices (room_id, deal_type, deposit, amount)
        VALUES (#{roomId}, #{price.dealType}, #{price.deposit}, #{price.amount})
        """)
    int insertPrice(@Param("roomId") Long roomId, @Param("price") Price price);
}
