package com.example.boards.service;

/**
 * 이메일 발송 서비스 인터페이스
 */
public interface EmailService {

    /**
     * 이메일 인증 메일 발송
     *
     * @param email  수신자 이메일
     * @param userId 사용자 ID
     * @param name   사용자 이름
     * @param token  인증 토큰
     */
    void sendVerificationEmail(String email, String userId, String name, String token);

    /**
     * 비밀번호 재설정 메일 발송 (향후 구현)
     *
     * @param email 수신자 이메일
     * @param token 재설정 토큰
     */
    void sendPasswordResetEmail(String email, String token);
}
