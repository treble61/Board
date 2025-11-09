package com.example.boards.mapper;

import com.example.boards.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    void insertUser(User user);
    User findByUserId(String userId);
    User findByEmail(String email);
    User findByUserIdAndPassword(String userId, String password);
    void updatePassword(@Param("userId") String userId, @Param("password") String password);
    void updateEmailVerified(@Param("userId") String userId);
}
