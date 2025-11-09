package com.example.boards.model;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 이메일 인증 토큰 모델
 *
 * 사용자 회원가입 시 생성되는 이메일 인증 토큰을 관리합니다.
 * 토큰은 UUID v4로 생성되며, 24시간의 유효기간을 가집니다.
 */
@Data
public class EmailVerificationToken {
    /**
     * 토큰 ID (기본 키)
     */
    private Long tokenId;

    /**
     * 사용자 ID (외래 키)
     */
    private String userId;

    /**
     * 인증 토큰 (UUID v4, 유니크)
     */
    private String token;

    /**
     * 토큰 만료 시간
     * 생성 시점 + 24시간
     */
    private Timestamp expiresAt;

    /**
     * 인증 완료 시간
     * NULL: 미사용
     * NOT NULL: 사용됨 (인증 완료)
     */
    private Timestamp verifiedAt;

    /**
     * 토큰 생성 시간
     */
    private Timestamp createdAt;

    /**
     * 토큰이 만료되었는지 확인
     * @return 만료 여부
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 토큰이 이미 사용되었는지 확인
     * @return 사용 여부
     */
    public boolean isUsed() {
        return verifiedAt != null;
    }

    /**
     * 토큰이 유효한지 확인 (만료되지 않았고 사용되지 않았는지)
     * @return 유효 여부
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }
}
