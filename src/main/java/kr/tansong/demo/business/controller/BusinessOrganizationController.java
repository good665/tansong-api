package kr.tansong.demo.business.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.admin.dto.OrganizationSummaryResponse;
import kr.tansong.demo.facility.mapper.OrganizationMapper;

/** 사업자 본인이 소유한 운영 기관 조회 (시설 등록 화면의 기관 선택용) */
@RestController
@RequestMapping("/api/business/organizations")
public class BusinessOrganizationController {

    private final OrganizationMapper organizationMapper;

    public BusinessOrganizationController(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    @GetMapping
    public List<OrganizationSummaryResponse> list(@AuthenticationPrincipal Long ownerUserId) {
        return organizationMapper.findAllOwnedByUser(ownerUserId);
    }
}
