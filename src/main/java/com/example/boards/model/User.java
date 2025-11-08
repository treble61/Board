package com.example.boards.model;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private String userId;
    private String password;
    private String name;
    private String email;
    private Date createdAt;
    private Date passwordChangedAt;
}
