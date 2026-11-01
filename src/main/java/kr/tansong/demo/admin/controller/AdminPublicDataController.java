package kr.tansong.demo.admin.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.publicdata.LtcImportService;

/** 공공데이터(장기요양기관) 시설 들여오기 — /api/admin/** 는 ROLE_ADMIN 전용 */
@RestController
@RequestMapping("/api/admin/public-data")
public class AdminPublicDataController {

    private final LtcImportService ltcImportService;

    public AdminPublicDataController(LtcImportService ltcImportService) {
        this.ltcImportService = ltcImportService;
    }

    // 예: POST /api/admin/public-data/ltc?siDoCd=11&limit=300  (서울 11, 경기 41)
    @PostMapping("/ltc")
    public LtcImportService.ImportResult importLtc(@RequestParam String siDoCd,
                                                   @RequestParam(defaultValue = "300") int limit) {
        return ltcImportService.importRegion(siDoCd, limit);
    }
}
