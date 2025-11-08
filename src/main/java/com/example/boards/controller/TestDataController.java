package com.example.boards.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:3000")
public class TestDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/reset-data")
    public ResponseEntity<?> resetTestData() {
        try {
            // 기존 게시글 삭제
            jdbcTemplate.execute("DELETE FROM posts");
            System.out.println("기존 게시글 삭제 완료");

            // 테스트 데이터 삽입
            ClassPathResource dataResource = new ClassPathResource("data.sql");
            String dataSql = new BufferedReader(new InputStreamReader(dataResource.getInputStream(), "UTF-8"))
                    .lines()
                    .collect(Collectors.joining("\n"));

            System.out.println("data.sql 파일 로드 완료");

            // 주석 제거 및 전체 SQL 정리
            StringBuilder cleanSql = new StringBuilder();
            for (String line : dataSql.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    cleanSql.append(line).append("\n");
                }
            }

            // SQL 문장을 세미콜론으로 분리 (보다 정교하게)
            String[] statements = cleanSql.toString().split(";");
            int executedCount = 0;

            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        jdbcTemplate.execute(trimmed);
                        executedCount++;
                        // 성공한 문장 로깅
                        if (trimmed.contains("INSERT INTO posts")) {
                            // VALUES 뒤의 데이터 행 수 카운트
                            int valueCount = trimmed.split("\\),\\s*\\(").length;
                            System.out.println(valueCount + "개의 게시글 추가");
                        }
                    } catch (Exception sqlEx) {
                        System.err.println("SQL 실행 실패: " + sqlEx.getMessage());
                        System.err.println("문장 시작: " + trimmed.substring(0, Math.min(200, trimmed.length())));
                    }
                }
            }

            System.out.println(executedCount + "개의 SQL 문장 실행 완료");

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM posts", Integer.class);
            System.out.println("최종 게시글 개수: " + count);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "테스트 데이터가 재생성되었습니다.");
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "테스트 데이터 생성 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
