package kr.tansong.demo.verification;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 공공데이터포털 "국세청_사업자등록정보 진위확인 및 상태조회 서비스"의 진위확인(validate) API.
 * 인증키는 환경변수 NTS_SERVICE_KEY 로 받습니다 (포털의 Decoding 키 — 여기서 URL 인코딩함).
 * 로그에 요청 URL 을 남기면 인증키가 노출되므로 예외 메시지는 기록하지 않습니다.
 */
@Component
public class NtsBusinessVerifier implements BusinessVerifier {

    private static final Logger log = LoggerFactory.getLogger(NtsBusinessVerifier.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;

    public NtsBusinessVerifier(RestClient.Builder builder,
                               ObjectMapper objectMapper,
                               @Value("${app.nts.base-url}") String baseUrl,
                               @Value("${app.nts.service-key:}") String serviceKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);

        this.restClient = builder.requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
    }

    @Override
    public VerificationResult verify(String businessNumber, String representativeName, LocalDate startDate) {
        if (serviceKey.isEmpty()) {
            log.warn("NTS_SERVICE_KEY 가 설정되지 않아 사업자 진위확인을 건너뜁니다.");
            return VerificationResult.unavailable("국세청 확인을 하지 못했습니다. (인증키 미설정)");
        }

        URI uri = URI.create(baseUrl + "/validate?returnType=JSON&serviceKey="
                + URLEncoder.encode(serviceKey, StandardCharsets.UTF_8));

        Map<String, Object> business = Map.of(
                "b_no", businessNumber,
                "start_dt", startDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                "p_nm", representativeName);

        try {
            // 길이를 명시해 보냄 (기본값인 chunked 전송을 거부하는 게이트웨이가 있음)
            byte[] payload = objectMapper.writeValueAsBytes(Map.of("businesses", List.of(business)));

            JsonNode body = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(payload.length)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            return parse(body);
        } catch (RestClientResponseException e) {
            log.error("국세청 진위확인 오류 응답: HTTP {} {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return VerificationResult.unavailable("국세청 확인을 하지 못했습니다. (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException | JsonProcessingException e) {
            log.error("국세청 진위확인 호출 실패: {}", e.getClass().getSimpleName());
            return VerificationResult.unavailable("국세청 확인을 하지 못했습니다. (연결 실패)");
        }
    }

    /** 응답 예: {"status_code":"OK","data":[{"valid":"01","status":{"b_stt_cd":"01",...}}]}
     *  valid 01 = 일치, 02 = 불일치 / b_stt_cd 01 = 계속사업자, 02 = 휴업, 03 = 폐업 */
    static VerificationResult parse(JsonNode body) {
        JsonNode item = body == null ? null : body.path("data").path(0);

        if (item == null || item.isMissingNode() || !item.has("valid")) {
            return VerificationResult.unavailable("국세청 응답을 해석하지 못했습니다.");
        }
        if (!"01".equals(item.path("valid").asText())) {
            return VerificationResult.mismatch();
        }

        String statusCode = item.path("status").path("b_stt_cd").asText("");
        if ("02".equals(statusCode) || "03".equals(statusCode)) {
            return VerificationResult.closed(statusCode);
        }
        return VerificationResult.verified(statusCode);
    }
}
