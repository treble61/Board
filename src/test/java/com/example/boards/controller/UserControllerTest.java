package com.example.boards.controller;

import com.example.boards.dto.LoginRequest;
import com.example.boards.dto.SignupRequest;
import com.example.boards.mapper.UserMapper;
import com.example.boards.model.User;
import com.example.boards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSignup_Success() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUserId("newuser");
        request.setPassword("password123");
        request.setPasswordConfirm("password123");
        request.setName("New User");
        request.setEmail("newuser@example.com");

        doNothing().when(userService).signup(any());

        // When & Then
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

        verify(userService).signup(any());
    }

    @Test
    void testSignup_Failure() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUserId("newuser");
        request.setPassword("password123");
        request.setPasswordConfirm("different123");
        request.setName("New User");
        request.setEmail("newuser@example.com");

        doThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."))
            .when(userService).signup(any());

        // When & Then
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUserId("testuser");
        request.setPassword("password123");

        User mockUser = new User();
        mockUser.setUserId("testuser");
        mockUser.setName("Test User");

        when(userService.login(any())).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.userName").value("Test User"));

        verify(userService).login(any());
    }

    @Test
    void testLogin_Failure() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUserId("testuser");
        request.setPassword("wrongpassword");

        when(userService.login(any()))
            .thenThrow(new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // When & Then
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("아이디 또는 비밀번호가 일치하지 않습니다."));
    }

    @Test
    void testLogout() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/users/logout")
                .session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        // Given
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", "testuser");
        session.setAttribute("userName", "Test User");

        User mockUser = new User();
        mockUser.setPasswordChangedAt(new Date());

        when(userMapper.findByUserId("testuser")).thenReturn(mockUser);
        when(userService.isPasswordChangeRequired(any())).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/users/me")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.userName").value("Test User"));
    }

    @Test
    void testGetCurrentUser_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}

