package com.example.boards.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "아이디는 필수입니다")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
