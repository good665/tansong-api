package kr.tansong.demo.store.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.tansong.demo.store.dto.StoreResponse;
import kr.tansong.demo.store.mapper.StoreMapper;

@Service
public class StoreService {

    private final StoreMapper storeMapper;

    public StoreService(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    public List<StoreResponse> getStores() {
        return storeMapper.findAll();
    }
}