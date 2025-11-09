package com.example.boards.service;

import com.example.boards.exception.TokenAlreadyUsedException;
import com.example.boards.exception.TokenExpiredException;
import com.example.boards.mapper.EmailVerificationTokenMapper;
import com.example.boards.mapper.UserMapper;
import com.example.boards.model.EmailVerificationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 이메일 인증 서비스
 *
 * 이메일 인증 토큰 생성, 검증, 인증 처리를 담당합니다.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    @Autowired
    private EmailVerificationTokenMapper tokenMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${app.mail.verification.expiry-hours:24}")
    private int expiryHours;

    /**
     * 인증 토큰 생성
     *
     * @param userId 사용자 ID
     * @return 생성된 토큰 문자열
     */
    @Transactional
    public String createVerificationToken(String userId) {
        // UUID v4 토큰 생성 (128비트 랜덤)
        String token = UUID.randomUUID().toString();

        // 만료 시간 계산 (현재 시간 + 설정된 시간)
        Timestamp expiresAt = Timestamp.from(
                Instant.now().plus(expiryHours, ChronoUnit.HOURS)
        );

        // 토큰 엔티티 생성
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUserId(userId);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(expiresAt);

        // DB에 저장
        tokenMapper.insertToken(verificationToken);

        log.info("Email verification token created: userId={}, expiresAt={}", userId, expiresAt);

        return token;
    }

    /**
     * 이메일 인증 처리
     *
     * 토큰을 검증하고 사용자의 이메일을 인증 완료 상태로 변경합니다.
     *
     * @param token 인증 토큰
     * @return 인증된 사용자 ID
     * @throws IllegalArgumentException 토큰이 존재하지 않음
     * @throws TokenExpiredException    토큰이 만료됨
     * @throws TokenAlreadyUsedException 토큰이 이미 사용됨
     */
    @Transactional
    public String verifyEmail(String token) {
        // 1. 토큰 조회
        EmailVerificationToken verificationToken = tokenMapper.findByToken(token);
        if (verificationToken == null) {
            log.warn("Invalid verification token: token={}", token);
            throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
        }

        // 2. 토큰 만료 확인
        if (verificationToken.isExpired()) {
            log.warn("Expired verification token: userId={}, token={}, expiresAt={}",
                    verificationToken.getUserId(), token, verificationToken.getExpiresAt());
            throw new TokenExpiredException("인증 토큰이 만료되었습니다. 인증 이메일을 재발송해주세요.");
        }

        // 3. 토큰 사용 여부 확인
        if (verificationToken.isUsed()) {
            log.warn("Already used verification token: userId={}, token={}, verifiedAt={}",
                    verificationToken.getUserId(), token, verificationToken.getVerifiedAt());
            throw new TokenAlreadyUsedException("이미 인증된 이메일입니다.");
        }

        String userId = verificationToken.getUserId();

        // 4. 사용자 이메일 인증 완료 처리
        userMapper.updateEmailVerified(userId);

        // 5. 토큰 사용 표시
        tokenMapper.updateVerifiedAt(token, new Timestamp(System.currentTimeMillis()));

        log.info("Email verified successfully: userId={}, token={}", userId, token);

        return userId;
    }

    /**
     * 사용자의 기존 미사용 토큰을 모두 만료 처리
     *
     * 인증 이메일 재발송 시 기존 토큰을 무효화하기 위해 사용합니다.
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void expireUnusedTokens(String userId) {
        tokenMapper.expireUnusedTokens(userId);
        log.info("Expired unused tokens for user: userId={}", userId);
    }

    /**
     * 토큰 유효성 확인 (인증 처리 없이 검증만 수행)
     *
     * @param token 토큰 문자열
     * @return 유효한 토큰 여부
     */
    public boolean isTokenValid(String token) {
        EmailVerificationToken verificationToken = tokenMapper.findByToken(token);
        return verificationToken != null && verificationToken.isValid();
    }
}
