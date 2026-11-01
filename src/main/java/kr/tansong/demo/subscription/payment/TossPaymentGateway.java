package kr.tansong.demo.subscription.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.tansong.demo.subscription.domain.BillingCard;

/**
 * 토스페이먼츠 자동결제(빌링). 키는 개발자센터의 "API 개별 연동 키"(test_ck/test_sk, 자동결제 MID)를
 * 환경변수 TOSS_CLIENT_KEY / TOSS_SECRET_KEY 로 받습니다. 시크릿 키가 있을 때만 활성화됩니다.
 * 토스는 스케줄링을 해주지 않으므로 매월 청구는 SubscriptionRenewalScheduler 가 직접 호출합니다.
 */
@Component
@ConditionalOnExpression("!'${app.toss.secret-key:}'.isBlank()")
public class TossPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentGateway.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String clientKey;

    public TossPaymentGateway(RestClient.Builder builder,
                              ObjectMapper objectMapper,
                              @Value("${app.toss.base-url}") String baseUrl,
                              @Value("${app.toss.client-key:}") String clientKey,
                              @Value("${app.toss.secret-key}") String secretKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(60_000); // 토스 문서: 자동결제 승인은 최대 60초

        // Basic 인증 = base64("시크릿키:") — 끝의 콜론이 빠지면 INCORRECT_BASIC_AUTH_FORMAT
        String basic = Base64.getEncoder().encodeToString((secretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));

        this.restClient = builder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Basic " + basic)
                .build();
        this.objectMapper = objectMapper;
        this.clientKey = clientKey.trim();
    }

    @Override
    public String providerName() {
        return "TOSS";
    }

    @Override
    public boolean requiresCard() {
        return true;
    }

    @Override
    public String clientKey() {
        return clientKey;
    }

    @Override
    public IssuedCard issueBillingKey(String customerKey, String authKey) {
        try {
            JsonNode body = post("/v1/billing/authorizations/issue",
                    Map.of("authKey", authKey, "customerKey", customerKey));
            String billingKey = body.path("billingKey").asText("");
            if (billingKey.isEmpty()) {
                throw new IllegalArgumentException("카드사 응답에 빌링키가 없습니다.");
            }
            return new IssuedCard(billingKey, body.path("card").path("number").asText(null));
        } catch (RestClientResponseException e) {
            throw new IllegalArgumentException("카드 등록에 실패했습니다. " + tossMessage(e));
        } catch (RestClientException | JsonProcessingException e) {
            log.error("토스 빌링키 발급 호출 실패: {}", e.getClass().getSimpleName());
            throw new IllegalArgumentException("카드 등록 중 결제사에 연결하지 못했습니다.");
        }
    }

    @Override
    public PaymentResult charge(BillingCard card, Long facilityId, int amount, String orderName) {
        if (card == null || card.getBillingKey() == null) {
            return PaymentResult.failure("등록된 결제 카드가 없습니다.");
        }

        // orderId: 6~64자, 영문/숫자/-/_/= 만 허용, 주문마다 고유
        String orderId = "sub-" + facilityId + "-" + UUID.randomUUID();

        try {
            JsonNode body = post("/v1/billing/" + card.getBillingKey(), Map.of(
                    "customerKey", card.getCustomerKey(),
                    "amount", amount,
                    "orderId", orderId,
                    "orderName", orderName));

            if (!"DONE".equals(body.path("status").asText())) {
                return PaymentResult.failure("결제가 완료되지 않았습니다. (" + body.path("status").asText() + ")");
            }
            return PaymentResult.success(body.path("paymentKey").asText(orderId));
        } catch (RestClientResponseException e) {
            String message = tossMessage(e);
            log.warn("토스 자동결제 거절 facilityId={} : {}", facilityId, message);
            return PaymentResult.failure(message);
        } catch (RestClientException | JsonProcessingException e) {
            // 응답을 못 받은 경우 — 실제로는 승인됐을 수 있으니 토스 상점관리자에서 orderId 로 확인 필요
            log.error("토스 자동결제 응답 없음 facilityId={} orderId={} : {}", facilityId, orderId, e.getClass().getSimpleName());
            return PaymentResult.failure("결제사 응답을 받지 못했습니다.");
        }
    }

    private JsonNode post(String path, Map<String, Object> body) throws JsonProcessingException {
        byte[] payload = objectMapper.writeValueAsBytes(body);
        return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(payload.length)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    /** 토스 오류 응답 {"code":"...","message":"..."} 에서 사용자에게 보여줄 메시지 */
    private String tossMessage(RestClientResponseException e) {
        try {
            JsonNode error = objectMapper.readTree(e.getResponseBodyAsString());
            String message = error.path("message").asText("");
            return message.isEmpty() ? "HTTP " + e.getStatusCode().value() : message;
        } catch (JsonProcessingException ignored) {
            return "HTTP " + e.getStatusCode().value();
        }
    }
}
