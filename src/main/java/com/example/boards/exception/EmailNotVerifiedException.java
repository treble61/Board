package com.example.boards.exception;

/**
 * 이메일 미인증 예외
 *
 * 이메일 인증이 완료되지 않은 사용자가 로그인 시도 시 발생합니다.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
