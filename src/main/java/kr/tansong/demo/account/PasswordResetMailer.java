package kr.tansong.demo.account;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SPRING_MAIL_HOST 를 설정해야 메일 발송 기능이 켜짐 */
@Component
public class PasswordResetMailer {

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public PasswordResetMailer(ObjectProvider<JavaMailSender> sender,
                               @Value("${app.mail.from:no-reply@seniorlife.invalid}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public boolean isConfigured() {
        return sender.getIfAvailable() != null;
    }

    public void send(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[시니어라이프] 비밀번호 재설정 코드");
        message.setText("""
            비밀번호 재설정 코드: %s

            앱의 비밀번호 찾기 화면에 입력해 주세요. 코드는 10분 동안 유효합니다.
            요청하지 않으셨다면 이 메일을 무시하셔도 됩니다. 비밀번호는 바뀌지 않습니다.
            """.formatted(code));
        sender.getObject().send(message);
    }
}
