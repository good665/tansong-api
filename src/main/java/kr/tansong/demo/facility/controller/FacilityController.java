
package kr.tansong.demo.facility.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.tansong.demo.facility.dto.FacilityResponse;
import kr.tansong.demo.facility.service.FacilityService;

@RestController
@RequestMapping("/api/facilities")
@CrossOrigin(origins = "http://localhost:3000")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public List<FacilityResponse> findAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {

        return facilityService.findAll(type, keyword);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getFacilityById(@PathVariable Long id) {
        FacilityResponse facility = facilityService.getFacilityById(id);

        if (facility == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(facility);
    }
}
