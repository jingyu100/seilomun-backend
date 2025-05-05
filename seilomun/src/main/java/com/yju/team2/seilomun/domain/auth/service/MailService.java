package com.yju.team2.seilomun.domain.auth.service;

import jakarta.mail.MessagingException; // jakarta.mail.MessagingException만 가져오기
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${senderEmail}")
    private String senderEmail;

    // Redis에 저장할 키 접두사
    private static final String EMAIL_AUTH_PREFIX = "EMAIL_AUTH:";
    // 인증 유효시간 (3분)
    private static final long EMAIL_AUTH_EXPIRATION = 3;

    // 랜덤으로 숫자 생성
    public int createNumber() {
        //(int) Math.random() * (최댓값-최소값+1) + 최소값
        return (int)(Math.random() * (900000)) + 100000;
    }

    public void sendAuthMail(String mail) {
        try {
            // 인증 번호 생성
            int authNumber = createNumber();
            // Redis에 저장 (key: EMAIL_AUTH:이메일, value: 인증번호)
            redisTemplate.opsForValue().set(
                    EMAIL_AUTH_PREFIX + mail,
                    String.valueOf(authNumber),
                    EMAIL_AUTH_EXPIRATION,
                    TimeUnit.MINUTES
            );

            MimeMessage message = createMail(mail, authNumber);
            mailSender.send(message);

            log.info("인증 메일 발송 완료: {}, 인증번호: {}", mail, authNumber);
        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("메일 발송 중 오류가 발생했습니다.", e);
        }
    }

    private MimeMessage createMail(String mail, int authNumber) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(mail);
        helper.setSubject("이메일 인증");

        String body = "";
        body += "<h3>" + "요청하신 인증 번호입니다." + "</h3>";
        body += "<h1>" + authNumber + "</h1>";
        body += "<h3>" + "감사합니다." + "</h3>";

        helper.setText(body, true);

        return message;
    }

    // 이메일 인증번호 검증
    public boolean verifyEmailAuth(String email, String authNumber) {
        String key = EMAIL_AUTH_PREFIX + email;
        String storedAuthNumber = redisTemplate.opsForValue().get(key);

        // 저장된 인증번호가 없거나 입력값과 다를 경우
        if (storedAuthNumber == null || !storedAuthNumber.equals(authNumber)) {
            return false;
        }

        // 인증 성공 시 Redis에서 해당 키 삭제
        redisTemplate.delete(key);
        return true;
    }
}