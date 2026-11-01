package kr.tansong.demo.verification;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.tansong.demo.common.NotFoundException;
import kr.tansong.demo.verification.mapper.BusinessVerificationMapper;

@Service
public class BusinessVerificationService {

    private final BusinessVerifier businessVerifier;
    private final BusinessVerificationMapper businessVerificationMapper;

    public BusinessVerificationService(BusinessVerifier businessVerifier,
                                       BusinessVerificationMapper businessVerificationMapper) {
        this.businessVerifier = businessVerifier;
        this.businessVerificationMapper = businessVerificationMapper;
    }

    public VerificationResult verify(String businessNumber, String representativeName, LocalDate startDate) {
        return businessVerifier.verify(businessNumber, representativeName, startDate);
    }

    public void record(Long organizationId, String representativeName, LocalDate startDate, VerificationResult result) {
        businessVerificationMapper.upsert(organizationId, representativeName, startDate,
                result.status().name(), result.ntsStatusCode(), result.message());
    }

    /** 관리자 재확인 — 가입 때 국세청 장애로 "미확인"이 된 기관을 다시 확인 */
    @Transactional
    public VerificationResult reverify(Long organizationId) {
        BusinessVerificationMapper.StoredInput input = businessVerificationMapper.findStoredInput(organizationId);

        if (input == null) {
            throw new NotFoundException("사업자 확인 정보가 없는 기관입니다. (진위확인 도입 전에 가입했거나 관리자가 등록한 기관)");
        }

        VerificationResult result = businessVerifier.verify(
                input.getBusinessNumber(), input.getRepresentativeName(), input.getStartDate());
        record(organizationId, input.getRepresentativeName(), input.getStartDate(), result);
        return result;
    }
}
