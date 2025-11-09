package com.example.boards.mapper;

import com.example.boards.model.EmailVerificationToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;

/**
 * 이메일 인증 토큰 Mapper
 *
 * email_verification_tokens 테이블에 대한 CRUD 작업을 정의합니다.
 */
@Mapper
public interface EmailVerificationTokenMapper {

    /**
     * 새로운 인증 토큰 삽입
     * @param token 인증 토큰 객체
     */
    void insertToken(EmailVerificationToken token);

    /**
     * 토큰으로 인증 토큰 조회
     * @param token 토큰 문자열
     * @return 인증 토큰 객체 (없으면 null)
     */
    EmailVerificationToken findByToken(@Param("token") String token);

    /**
     * 사용자 ID로 인증 토큰 목록 조회
     * @param userId 사용자 ID
     * @return 인증 토큰 목록
     */
    java.util.List<EmailVerificationToken> findByUserId(@Param("userId") String userId);

    /**
     * 사용자 ID로 미사용 토큰 조회 (만료 여부 무관)
     * @param userId 사용자 ID
     * @return 미사용 토큰 목록
     */
    java.util.List<EmailVerificationToken> findUnusedTokensByUserId(@Param("userId") String userId);

    /**
     * 토큰 인증 완료 표시
     * @param token 토큰 문자열
     * @param verifiedAt 인증 완료 시간
     */
    void updateVerifiedAt(@Param("token") String token, @Param("verifiedAt") Timestamp verifiedAt);

    /**
     * 사용자의 모든 미사용 토큰을 만료 처리 (재발송 시 사용)
     * @param userId 사용자 ID
     */
    void expireUnusedTokens(@Param("userId") String userId);

    /**
     * 만료된 토큰 삭제 (정리 작업용)
     * @param expiryThreshold 삭제할 토큰의 만료 시간 기준 (예: 7일 이전)
     * @return 삭제된 행 수
     */
    int deleteExpiredTokens(@Param("expiryThreshold") Timestamp expiryThreshold);
}
