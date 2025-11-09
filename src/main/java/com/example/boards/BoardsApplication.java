package com.example.boards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BoardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardsApplication.class, args);
    }
}
