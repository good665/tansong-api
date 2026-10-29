package kr.tansong.demo.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.admin.dto.OrganizationCreateRequest;
import kr.tansong.demo.admin.dto.OrganizationSummaryResponse;
import kr.tansong.demo.facility.mapper.OrganizationMapper;

@Service
public class AdminOrganizationService {

    private final OrganizationMapper organizationMapper;

    public AdminOrganizationService(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    public List<OrganizationSummaryResponse> list() {
        return organizationMapper.findAllActive();
    }

    @Transactional
    public Long create(OrganizationCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("운영 기관 정보가 없습니다.");
        }

        String name = trim(request.getName());
        String organizationType = trim(request.getOrganizationType());
        String phone = trim(request.getPhone());
        String email = trim(request.getEmail());

        if (name.isEmpty() || name.length() > 150) {
            throw new IllegalArgumentException("기관명을 1~150자로 입력해 주세요.");
        }
        if (organizationType.isEmpty() || organizationType.length() > 40) {
            throw new IllegalArgumentException("기관 유형을 1~40자로 입력해 주세요.");
        }

        request.setName(name);
        request.setOrganizationType(organizationType);
        request.setPhone(phone.isEmpty() ? null : phone);
        request.setEmail(email.isEmpty() ? null : email);

        organizationMapper.insert(request);
        return request.getId();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
