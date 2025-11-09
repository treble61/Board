package com.example.boards.exception;

/**
 * 토큰 이미 사용됨 예외
 *
 * 이미 사용된 이메일 인증 토큰을 재사용하려 할 때 발생합니다.
 */
public class TokenAlreadyUsedException extends RuntimeException {
    public TokenAlreadyUsedException(String message) {
        super(message);
    }
}
