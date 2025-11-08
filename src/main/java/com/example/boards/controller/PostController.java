package com.example.boards.controller;

import com.example.boards.model.Post;
import com.example.boards.service.PostService;
import com.example.boards.util.ExcelValidator;
import com.example.boards.util.FilePathSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    private final String uploadDir = "uploads";

    public PostController() {
        // Create uploads directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        int offset = (page - 1) * size;
        List<Post> posts = postService.getAllPosts(size, offset, search);
        int totalCount = postService.getTotalCount(search);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts);
        response.put("totalCount", totalCount);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("pageSize", size);

        System.out.println("=== 게시글 목록 조회 ===");
        System.out.println("검색어: " + (search != null ? search : "없음"));
        System.out.println("페이지: " + page + " / " + totalPages);
        System.out.println("조회된 게시글: " + posts.size() + "개");
        System.out.println("전체 게시글: " + totalCount + "개");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Post post, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        post.setAuthorId(userId);
        if (post.getIsNotice() == null) {
            post.setIsNotice(false);
        }

        postService.createPost(post);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(@PathVariable Long postId, @RequestBody Post post, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        Post existingPost = postService.getPostById(postId);
        if (!existingPost.getAuthorId().equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "수정 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        post.setPostId(postId);
        post.setAuthorId(userId);
        postService.updatePost(post);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        Post existingPost = postService.getPostById(postId);
        if (!existingPost.getAuthorId().equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "삭제 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        postService.deletePost(postId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "게시글이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    // Excel file upload (separate from regular attachments, 1 per post)
    @PostMapping("/{postId}/excel")
    public ResponseEntity<?> uploadExcel(@PathVariable Long postId,
                                        @RequestParam("file") MultipartFile file,
                                        HttpSession session) {
        System.out.println("=== 엑셀 파일 업로드 시작 ===");
        System.out.println("postId: " + postId);
        System.out.println("파일명: " + file.getOriginalFilename());
        System.out.println("파일 크기: " + file.getSize() + " bytes");

        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            System.out.println("ERROR: 로그인 필요");
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        // Verify post exists and user has permission
        Post existingPost = postService.getPostById(postId);
        if (!existingPost.getAuthorId().equals(userId)) {
            System.out.println("ERROR: 권한 없음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일을 업로드할 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        if (file.isEmpty()) {
            System.out.println("ERROR: 파일이 비어있음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일이 비어있습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        String originalFilename = file.getOriginalFilename();

        // Validate Excel file
        if (!ExcelValidator.isExcelFile(originalFilename)) {
            System.out.println("ERROR: 엑셀 파일이 아님");
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일만 업로드 가능합니다. (.xlsx, .xls)");
            return ResponseEntity.badRequest().body(error);
        }

        // Check file size (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            System.out.println("ERROR: 파일 크기 초과");
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일 크기는 10MB를 초과할 수 없습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate Excel file with POI
        try (InputStream inputStream = file.getInputStream()) {
            Map<String, Object> validationResult = ExcelValidator.validateExcelFile(inputStream, originalFilename);

            if (!(Boolean) validationResult.get("isValid")) {
                System.out.println("ERROR: 엑셀 파일 검증 실패");
                Map<String, String> error = new HashMap<>();
                error.put("error", (String) validationResult.get("error"));
                return ResponseEntity.badRequest().body(error);
            }

            System.out.println("엑셀 파일 검증 성공!");
            System.out.println("- 시트 개수: " + validationResult.get("numberOfSheets"));
            System.out.println("- 파일 형식: " + validationResult.get("fileType"));
        } catch (IOException e) {
            System.out.println("ERROR: 엑셀 파일 읽기 실패");
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일을 읽을 수 없습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Delete old Excel file if exists
            if (existingPost.getExcelStoredFilename() != null) {
                Path oldFilePath = Paths.get(uploadDir, existingPost.getExcelStoredFilename());
                Files.deleteIfExists(oldFilePath);
                System.out.println("기존 엑셀 파일 삭제: " + oldFilePath.toString());
            }

            // Save new file with path traversal protection
            Path filePath = FilePathSanitizer.sanitizeFilePath(uploadDir, originalFilename);
            String storedFilename = filePath.getFileName().toString();
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("엑셀 파일 저장 완료: " + filePath.toString());

            // Update database
            postService.updateExcelFile(postId, originalFilename, storedFilename,
                                       filePath.toString(), file.getSize());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "엑셀 파일이 업로드되었습니다.");
            response.put("filename", originalFilename);
            response.put("fileSize", file.getSize());
            System.out.println("=== 엑셀 파일 업로드 성공 ===");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            System.out.println("ERROR: 파일 저장 실패 - " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일 업로드에 실패했습니다.");
            return ResponseEntity.status(500).body(error);
        }
    }

    // Excel file download
    @GetMapping("/{postId}/excel/download")
    public ResponseEntity<?> downloadExcel(@PathVariable Long postId, HttpSession session) {
        System.out.println("=== 엑셀 파일 다운로드 ===");
        System.out.println("postId: " + postId);

        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            System.out.println("ERROR: 로그인 필요");
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        Post post = postService.getPostById(postId);
        if (post.getExcelStoredFilename() == null) {
            System.out.println("ERROR: 엑셀 파일이 없음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "업로드된 엑셀 파일이 없습니다.");
            return ResponseEntity.notFound().build();
        }

        // AUTHORIZATION CHECK: Verify user owns the post
        if (!post.getAuthorId().equals(userId)) {
            System.out.println("ERROR: 권한 없음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일 다운로드 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        try {
            Path filePath = Paths.get(post.getExcelFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                System.out.println("엑셀 파일 다운로드 시작: " + post.getExcelFilename());
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + post.getExcelFilename() + "\"")
                        .body(resource);
            } else {
                System.out.println("ERROR: 파일을 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            System.out.println("ERROR: 잘못된 파일 경로 - " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Excel file delete
    @DeleteMapping("/{postId}/excel")
    public ResponseEntity<?> deleteExcel(@PathVariable Long postId, HttpSession session) {
        System.out.println("=== 엑셀 파일 삭제 ===");
        System.out.println("postId: " + postId);

        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            System.out.println("ERROR: 로그인 필요");
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        Post existingPost = postService.getPostById(postId);
        if (!existingPost.getAuthorId().equals(userId)) {
            System.out.println("ERROR: 권한 없음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일을 삭제할 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        if (existingPost.getExcelStoredFilename() == null) {
            System.out.println("ERROR: 엑셀 파일이 없음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "삭제할 엑셀 파일이 없습니다.");
            return ResponseEntity.notFound().build();
        }

        try {
            // Delete physical file
            Path filePath = Paths.get(existingPost.getExcelFilePath());
            Files.deleteIfExists(filePath);
            System.out.println("엑셀 파일 삭제: " + filePath.toString());

            // Update database
            postService.deleteExcelFile(postId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "엑셀 파일이 삭제되었습니다.");
            System.out.println("=== 엑셀 파일 삭제 성공 ===");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            System.out.println("ERROR: 파일 삭제 실패 - " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "엑셀 파일 삭제에 실패했습니다.");
            return ResponseEntity.status(500).body(error);
        }
    }
}
