package com.example.boards.exception;

/**
 * 토큰 만료 예외
 *
 * 이메일 인증 토큰이 만료되었을 때 발생합니다.
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
