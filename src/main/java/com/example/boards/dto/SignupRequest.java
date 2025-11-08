package com.example.boards.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String userId;
    private String password;
    private String passwordConfirm;
    private String name;
    private String email;
}
