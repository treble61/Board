package com.example.boards.service.impl;

import com.example.boards.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 이메일 발송 서비스 구현
 *
 * JavaMailSender를 사용하여 HTML 이메일을 발송합니다.
 * 비동기 처리를 통해 응답 시간을 최소화합니다.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.verification.base-url}")
    private String baseUrl;

    /**
     * 이메일 인증 메일 발송 (비동기)
     *
     * @param email  수신자 이메일
     * @param userId 사용자 ID
     * @param name   사용자 이름
     * @param token  인증 토큰
     */
    @Async
    @Override
    public void sendVerificationEmail(String email, String userId, String name, String token) {
        try {
            String verificationUrl = baseUrl + "/verify-email?token=" + token;
            String subject = "[게시판] 이메일 인증을 완료해주세요";
            String htmlContent = buildVerificationEmailHtml(name, verificationUrl);

            sendHtmlEmail(email, subject, htmlContent);

            log.info("Verification email sent successfully: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("Failed to send verification email: userId={}, email={}", userId, email, e);
            // 이메일 발송 실패는 회원가입을 막지 않음 (비즈니스 결정)
            // 필요시 재발송 기능 사용
        }
    }

    /**
     * 비밀번호 재설정 메일 발송 (향후 구현)
     *
     * @param email 수신자 이메일
     * @param token 재설정 토큰
     */
    @Override
    public void sendPasswordResetEmail(String email, String token) {
        // TODO: 향후 구현
        throw new UnsupportedOperationException("비밀번호 재설정 기능은 아직 구현되지 않았습니다.");
    }

    /**
     * HTML 이메일 발송
     *
     * @param to      수신자
     * @param subject 제목
     * @param html    HTML 본문
     * @throws MessagingException 이메일 발송 실패 시
     */
    private void sendHtmlEmail(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true); // true = HTML 이메일

        mailSender.send(message);
    }

    /**
     * 이메일 인증 HTML 템플릿 생성
     *
     * @param name            사용자 이름
     * @param verificationUrl 인증 URL
     * @return HTML 문자열
     */
    private String buildVerificationEmailHtml(String name, String verificationUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 30px 20px; background-color: #f9f9f9; }" +
                "        .button {" +
                "            display: inline-block;" +
                "            padding: 12px 24px;" +
                "            background-color: #007bff;" +
                "            color: white;" +
                "            text-decoration: none;" +
                "            border-radius: 4px;" +
                "            font-weight: bold;" +
                "        }" +
                "        .button:hover { background-color: #0056b3; }" +
                "        .link-box {" +
                "            word-break: break-all;" +
                "            background-color: #ffffff;" +
                "            padding: 10px;" +
                "            border: 1px solid #ddd;" +
                "            border-radius: 4px;" +
                "            margin: 20px 0;" +
                "        }" +
                "        .footer {" +
                "            margin-top: 30px;" +
                "            padding-top: 20px;" +
                "            border-top: 1px solid #ddd;" +
                "            font-size: 12px;" +
                "            color: #666;" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h2>이메일 인증</h2>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <p>안녕하세요, <strong>" + name + "</strong>님</p>" +
                "            <p>회원가입을 완료하려면 아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>" +
                "            <p style=\"text-align: center; margin: 30px 0;\">" +
                "                <a href=\"" + verificationUrl + "\" class=\"button\">이메일 인증하기</a>" +
                "            </p>" +
                "            <p>또는 아래 링크를 복사하여 브라우저에 붙여넣으세요:</p>" +
                "            <div class=\"link-box\">" + verificationUrl + "</div>" +
                "            <div class=\"footer\">" +
                "                <p>이 링크는 24시간 동안 유효합니다.</p>" +
                "                <p>본인이 요청하지 않은 경우 이 이메일을 무시하셔도 됩니다.</p>" +
                "            </div>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}
