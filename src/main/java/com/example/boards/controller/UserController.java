package com.example.boards.controller;

import com.example.boards.dto.ChangePasswordRequest;
import com.example.boards.dto.LoginRequest;
import com.example.boards.dto.SignupRequest;
import com.example.boards.mapper.UserMapper;
import com.example.boards.model.User;
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
            Map<String, String> response = new HashMap<>();
            response.put("message", "회원가입이 완료되었습니다.");
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
}
