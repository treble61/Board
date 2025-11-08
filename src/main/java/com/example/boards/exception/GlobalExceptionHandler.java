package com.example.boards.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 * 애플리케이션 전체에서 발생하는 예외를 중앙에서 처리하여
 * 일관된 오류 응답을 제공하고 민감한 정보 노출을 방지합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 검증 실패 예외 처리 (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed for request: {} - {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "잘못된 값입니다.",
                (existing, replacement) -> existing  // Keep first error for duplicate fields
            ));

        Map<String, Object> response = new HashMap<>();
        response.put("error", "입력값 검증에 실패했습니다.");
        response.put("fields", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 비즈니스 로직 검증 실패 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Business validation failed for request: {} - {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> response = new HashMap<>();
        // 사용자에게 안전한 검증 오류 메시지만 노출
        response.put("error", ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 권한 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied for request: {} - User: {}",
            request.getRequestURI(),
            request.getRemoteUser());

        Map<String, String> response = new HashMap<>();
        response.put("error", "접근 권한이 없습니다.");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 보안 예외 처리 (경로 순회 등)
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(
            SecurityException ex,
            HttpServletRequest request) {

        log.error("Security violation detected for request: {} - {}",
            request.getRequestURI(),
            ex.getMessage());

        Map<String, String> response = new HashMap<>();
        response.put("error", "보안 정책 위반이 감지되었습니다.");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 일반적인 예외 처리
     * 상세한 오류 정보는 로그에만 기록하고, 사용자에게는 일반적인 메시지만 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // 상세한 오류 정보는 서버 로그에만 기록
        log.error("Unexpected error occurred for request: {} - Error: {}",
            request.getRequestURI(),
            ex.getMessage(),
            ex);

        Map<String, String> response = new HashMap<>();
        // 사용자에게는 일반적인 오류 메시지만 반환 (정보 유출 방지)
        response.put("error", "요청을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * NullPointerException 처리
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, String>> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {

        log.error("NullPointerException occurred for request: {} - {}",
            request.getRequestURI(),
            ex.getMessage(),
            ex);

        Map<String, String> response = new HashMap<>();
        response.put("error", "요청을 처리할 수 없습니다.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
