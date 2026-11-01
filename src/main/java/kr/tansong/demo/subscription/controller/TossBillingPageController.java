package kr.tansong.demo.subscription.controller;

import java.util.regex.Pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.tansong.demo.subscription.service.BillingCardService;

/** 브라우저로 여는 카드 등록 페이지와 토스의 success/fail 리다이렉트 (로그인 없이 접근 — SecurityConfig 참고) */
@RestController
@RequestMapping(value = "/billing/toss", produces = "text/html;charset=UTF-8")
public class TossBillingPageController {

    private static final Pattern UUID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final BillingCardService billingCardService;
    private final ObjectMapper objectMapper;

    public TossBillingPageController(BillingCardService billingCardService, ObjectMapper objectMapper) {
        this.billingCardService = billingCardService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String customerKey) throws JsonProcessingException {
        // 스크립트에 그대로 넣는 값이라 형식을 먼저 확인 (우리가 만든 UUID 만 통과)
        if (customerKey == null || !UUID.matcher(customerKey).matches()
                || !billingCardService.isPendingCustomerKey(customerKey)) {
            return page("카드 등록", "만료되었거나 잘못된 요청입니다. 앱에서 다시 시도해 주세요.");
        }

        return """
            <!doctype html>
            <html lang="ko"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>결제 카드 등록</title>
            <script src="https://js.tosspayments.com/v2/standard"></script>
            </head>
            <body style="font-family:sans-serif;padding:24px">
            <p id="msg">결제 카드 등록 화면을 여는 중입니다…</p>
            <script>
            (async function () {
              try {
                var payment = TossPayments(%s).payment({ customerKey: %s });
                // 이 페이지를 연 주소 그대로 돌아오게 함 (기기마다 서버 주소가 다름)
                await payment.requestBillingAuth({
                  method: "CARD",
                  successUrl: location.origin + "/billing/toss/success",
                  failUrl: location.origin + "/billing/toss/fail"
                });
              } catch (e) {
                document.getElementById("msg").textContent = "카드 등록을 시작하지 못했습니다: " + e.message;
              }
            })();
            </script>
            </body></html>
            """.formatted(
                json(billingCardService.clientKey()),
                json(customerKey));
    }

    @GetMapping("/success")
    public String success(@RequestParam(required = false) String customerKey,
                          @RequestParam(required = false) String authKey) {
        if (customerKey == null || authKey == null) {
            return page("카드 등록 실패", "필수 정보가 빠진 요청입니다.");
        }

        try {
            billingCardService.completeRegistration(customerKey, authKey);
            return page("카드 등록 완료", "결제 카드가 등록되었습니다. 이 창을 닫고 앱으로 돌아가 주세요.");
        } catch (IllegalArgumentException e) {
            return page("카드 등록 실패", e.getMessage());
        }
    }

    @GetMapping("/fail")
    public String fail(@RequestParam(required = false) String code,
                       @RequestParam(required = false) String message) {
        String reason = "PAY_PROCESS_CANCELED".equals(code) ? "카드 등록을 취소했습니다." : (message == null ? "카드 등록에 실패했습니다." : message);
        return page("카드 등록 실패", reason + " 이 창을 닫고 앱에서 다시 시도해 주세요.");
    }

    private String page(String title, String message) {
        return """
            <!doctype html>
            <html lang="ko"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title></head>
            <body style="font-family:sans-serif;padding:24px"><h2>%s</h2><p>%s</p></body></html>
            """.formatted(HtmlUtils.htmlEscape(title), HtmlUtils.htmlEscape(title), HtmlUtils.htmlEscape(message));
    }

    private String json(String value) throws JsonProcessingException {
        // </script> 로 스크립트를 끊지 못하게 '<' 도 이스케이프
        return objectMapper.writeValueAsString(value).replace("<", "\\u003c");
    }
}
