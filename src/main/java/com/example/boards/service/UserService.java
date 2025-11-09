package com.example.boards.service;

import com.example.boards.dto.ChangePasswordRequest;
import com.example.boards.dto.LoginRequest;
import com.example.boards.dto.SignupRequest;
import com.example.boards.exception.EmailNotVerifiedException;
import com.example.boards.mapper.UserMapper;
import com.example.boards.model.User;
import com.example.boards.util.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailService emailService;

    public void signup(SignupRequest request) {
        // 비밀번호 확인 검증
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 중복 아이디 검증
        User existingUser = userMapper.findByUserId(request.getUserId());
        if (existingUser != null) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 중복 이메일 검증 (추가)
        User existingEmailUser = userMapper.findByEmail(request.getEmail());
        if (existingEmailUser != null) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 사용자 생성 (email_verified = false로 자동 설정됨)
        User user = new User();
        user.setUserId(request.getUserId());
        user.setPassword(PasswordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        userMapper.insertUser(user);
        log.info("User created: userId={}, email={}", user.getUserId(), user.getEmail());

        // 이메일 인증 토큰 생성
        String token = emailVerificationService.createVerificationToken(user.getUserId());

        // 인증 이메일 발송 (비동기)
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getUserId(),
                user.getName(),
                token
        );

        log.info("Signup completed: userId={}, email={}", user.getUserId(), user.getEmail());
    }

    public User login(LoginRequest request) {
        String encodedPassword = PasswordEncoder.encode(request.getPassword());
        User user = userMapper.findByUserIdAndPassword(request.getUserId(), encodedPassword);

        if (user == null) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 이메일 인증 확인
        if (user.getEmailVerified() == null || !user.getEmailVerified()) {
            log.warn("Login attempted with unverified email: userId={}", user.getUserId());
            throw new EmailNotVerifiedException("이메일 인증이 필요합니다. 이메일을 확인해주세요.");
        }

        log.info("Login successful: userId={}", user.getUserId());
        return user;
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        // 비밀번호 확인 검증
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호 확인
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        String encodedCurrentPassword = PasswordEncoder.encode(request.getCurrentPassword());
        if (!user.getPassword().equals(encodedCurrentPassword)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호로 업데이트
        String encodedNewPassword = PasswordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userId, encodedNewPassword);
    }

    public boolean isPasswordChangeRequired(User user) {
        if (user.getPasswordChangedAt() == null) {
            return true; // 비밀번호 변경 이력이 없으면 변경 필요
        }

        // 3개월(90일) 경과 여부 확인
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(user.getPasswordChangedAt());
        calendar.add(Calendar.DAY_OF_MONTH, 90);
        Date expiryDate = calendar.getTime();

        return new Date().after(expiryDate);
    }

    /**
     * 인증 이메일 재발송
     *
     * @param email 사용자 이메일
     */
    public void resendVerificationEmail(String email) {
        // 이메일로 사용자 조회
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("등록되지 않은 이메일입니다.");
        }

        // 이미 인증된 사용자 확인
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new IllegalArgumentException("이미 인증된 이메일입니다.");
        }

        // 기존 미사용 토큰 만료 처리
        emailVerificationService.expireUnusedTokens(user.getUserId());

        // 새 인증 토큰 생성
        String token = emailVerificationService.createVerificationToken(user.getUserId());

        // 인증 이메일 재발송 (비동기)
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getUserId(),
                user.getName(),
                token
        );

        log.info("Verification email resent: userId={}, email={}", user.getUserId(), user.getEmail());
    }
}
