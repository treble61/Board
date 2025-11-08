package com.example.boards.service;

import com.example.boards.dto.ChangePasswordRequest;
import com.example.boards.dto.LoginRequest;
import com.example.boards.dto.SignupRequest;
import com.example.boards.mapper.UserMapper;
import com.example.boards.model.User;
import com.example.boards.util.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("testuser");
        testUser.setPassword(PasswordEncoder.encode("password123"));
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(new Date());
    }

    @Test
    void testSignup_Success() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUserId("newuser");
        request.setPassword("password123");
        request.setPasswordConfirm("password123");
        request.setName("New User");
        request.setEmail("newuser@example.com");

        when(userMapper.findByUserId("newuser")).thenReturn(null);

        // When
        assertDoesNotThrow(() -> userService.signup(request));

        // Then
        verify(userMapper).findByUserId("newuser");
        verify(userMapper).insertUser(any(User.class));
    }

    @Test
    void testSignup_PasswordMismatch() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUserId("newuser");
        request.setPassword("password123");
        request.setPasswordConfirm("different123");
        request.setName("New User");
        request.setEmail("newuser@example.com");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.signup(request)
        );
        assertEquals("비밀번호가 일치하지 않습니다.", exception.getMessage());
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    void testSignup_DuplicateUserId() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUserId("testuser");
        request.setPassword("password123");
        request.setPasswordConfirm("password123");
        request.setName("New User");
        request.setEmail("newuser@example.com");

        when(userMapper.findByUserId("testuser")).thenReturn(testUser);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.signup(request)
        );
        assertEquals("이미 존재하는 아이디입니다.", exception.getMessage());
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUserId("testuser");
        request.setPassword("password123");

        when(userMapper.findByUserIdAndPassword("testuser", PasswordEncoder.encode("password123")))
            .thenReturn(testUser);

        // When
        User result = userService.login(request);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUserId());
        verify(userMapper).findByUserIdAndPassword("testuser", anyString());
    }

    @Test
    void testLogin_WrongCredentials() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUserId("testuser");
        request.setPassword("wrongpassword");

        when(userMapper.findByUserIdAndPassword(anyString(), anyString())).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.login(request)
        );
        assertEquals("아이디 또는 비밀번호가 일치하지 않습니다.", exception.getMessage());
    }

    @Test
    void testChangePassword_Success() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword456");
        request.setNewPasswordConfirm("newpassword456");

        when(userMapper.findByUserId("testuser")).thenReturn(testUser);

        // When
        assertDoesNotThrow(() -> userService.changePassword("testuser", request));

        // Then
        verify(userMapper).findByUserId("testuser");
        verify(userMapper).updatePassword(eq("testuser"), anyString());
    }

    @Test
    void testChangePassword_NewPasswordMismatch() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword456");
        request.setNewPasswordConfirm("different456");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword("testuser", request)
        );
        assertEquals("새 비밀번호가 일치하지 않습니다.", exception.getMessage());
        verify(userMapper, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpassword");
        request.setNewPassword("newpassword456");
        request.setNewPasswordConfirm("newpassword456");

        when(userMapper.findByUserId("testuser")).thenReturn(testUser);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword("testuser", request)
        );
        assertEquals("현재 비밀번호가 일치하지 않습니다.", exception.getMessage());
        verify(userMapper, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void testIsPasswordChangeRequired_WhenNull() {
        // Given
        testUser.setPasswordChangedAt(null);

        // When
        boolean result = userService.isPasswordChangeRequired(testUser);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPasswordChangeRequired_WhenRecentlyChanged() {
        // Given
        testUser.setPasswordChangedAt(new Date()); // Just changed

        // When
        boolean result = userService.isPasswordChangeRequired(testUser);

        // Then
        assertFalse(result);
    }
}

