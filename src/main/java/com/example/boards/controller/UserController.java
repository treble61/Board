package com.example.boards.controller;

import com.example.boards.dto.ChangePasswordRequest;
import com.example.boards.dto.LoginRequest;
import com.example.boards.dto.ResendVerificationRequest;
import com.example.boards.dto.SignupRequest;
import com.example.boards.exception.EmailNotVerifiedException;
import com.example.boards.exception.TokenAlreadyUsedException;
import com.example.boards.exception.TokenExpiredException;
import com.example.boards.mapper.UserMapper;
import com.example.boards.model.User;
import com.example.boards.service.EmailVerificationService;
import com.example.boards.service.RateLimiterService;
import com.example.boards.service.UserService;
import com.example.boards.util.IpAddressUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        // Rate limiting check
        String clientIp = IpAddressUtil.getClientIpAddress(httpRequest);
        if (!rateLimiterService.allowSignup(clientIp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "너무 많은 회원가입 시도가 있었습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            userService.signup(request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "회원가입이 완료되었습니다. 이메일을 확인하여 인증을 완료해주세요.");
            response.put("email", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpSession session, HttpServletRequest httpRequest) {
        // Rate limiting check
        String clientIp = IpAddressUtil.getClientIpAddress(httpRequest);
        if (!rateLimiterService.allowLogin(clientIp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "너무 많은 로그인 시도가 있었습니다. 15분 후 다시 시도해주세요.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            User user = userService.login(request);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userName", user.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인 성공");
            response.put("userId", user.getUserId());
            response.put("userName", user.getName());
            return ResponseEntity.ok(response);
        } catch (EmailNotVerifiedException e) {
            // 이메일 미인증 에러는 403 Forbidden으로 반환
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("emailVerificationRequired", true);
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃 되었습니다.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        // 비밀번호 변경 필요 여부 확인
        User user = userMapper.findByUserId(userId);
        boolean passwordChangeRequired = userService.isPasswordChangeRequired(user);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("userName", userName);
        response.put("passwordChangeRequired", passwordChangeRequired);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpSession session, HttpServletRequest httpRequest) {
        // Rate limiting check
        String clientIp = IpAddressUtil.getClientIpAddress(httpRequest);
        if (!rateLimiterService.allowPasswordChange(clientIp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "너무 많은 비밀번호 변경 시도가 있었습니다. 15분 후 다시 시도해주세요.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }

            userService.changePassword(userId, request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "비밀번호가 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 이메일 인증 엔드포인트
     *
     * @param token 인증 토큰 (쿼리 파라미터)
     * @return 인증 결과
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token, HttpServletRequest httpRequest) {
        // Rate limiting check
        String clientIp = IpAddressUtil.getClientIpAddress(httpRequest);
        if (!rateLimiterService.allowEmailVerification(clientIp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "너무 많은 인증 시도가 있었습니다. 1시간 후 다시 시도해주세요.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            String userId = emailVerificationService.verifyEmail(token);

            Map<String, String> response = new HashMap<>();
            response.put("message", "이메일 인증이 완료되었습니다. 로그인해주세요.");
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } catch (TokenExpiredException e) {
            // 410 Gone - 토큰 만료
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(410).body(error);
        } catch (TokenAlreadyUsedException e) {
            // 409 Conflict - 이미 사용됨
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(409).body(error);
        } catch (IllegalArgumentException e) {
            // 400 Bad Request - 유효하지 않은 토큰
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "이메일 인증 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 인증 이메일 재발송 엔드포인트
     *
     * @param request 재발송 요청 (이메일)
     * @return 재발송 결과
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        // Rate limiting check - 이메일 기준
        if (!rateLimiterService.allowResendVerification(request.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "너무 많은 재발송 요청이 있었습니다. 1시간 후 다시 시도해주세요.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            userService.resendVerificationEmail(request.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("message", "인증 이메일이 재전송되었습니다. 이메일을 확인해주세요.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "인증 이메일 재발송 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(error);
        }
    }
}
