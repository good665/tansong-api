package kr.tansong.demo.store.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.tansong.demo.store.dto.StoreResponse;

@Mapper
public interface StoreMapper {

    List<StoreResponse> findAll();

}
