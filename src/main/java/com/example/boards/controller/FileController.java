package com.example.boards.controller;

import com.example.boards.model.FileAttachment;
import com.example.boards.service.FileAttachmentService;
import com.example.boards.util.ExcelValidator;
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
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    @Autowired
    private FileAttachmentService fileAttachmentService;

    private final String uploadDir = "uploads";

    public FileController() {
        // Create uploads directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<FileAttachment>> getFilesByPostId(@PathVariable Long postId) {
        System.out.println("=== 첨부파일 조회 ===");
        System.out.println("postId: " + postId);
        List<FileAttachment> files = fileAttachmentService.getFilesByPostId(postId);
        System.out.println("조회된 파일 개수: " + files.size());
        for (FileAttachment file : files) {
            System.out.println("  - " + file.getOriginalFilename() + " (" + file.getFileSize() + " bytes)");
        }
        return ResponseEntity.ok(files);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("postId") Long postId,
                                        HttpSession session) {
        System.out.println("=== 파일 업로드 시작 ===");
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

        if (file.isEmpty()) {
            System.out.println("ERROR: 파일이 비어있음");
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일이 비어있습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        // Check file size (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            System.out.println("ERROR: 파일 크기 초과");
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일 크기는 10MB를 초과할 수 없습니다.");
            return ResponseEntity.badRequest().body(error);
        }

        String originalFilename = file.getOriginalFilename();

        // 엑셀 파일인 경우 POI로 검증
        if (ExcelValidator.isExcelFile(originalFilename)) {
            System.out.println("엑셀 파일 감지, 유효성 검증 시작...");
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
        }

        try {
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = Paths.get(uploadDir, storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("파일 저장 완료: " + filePath.toString());

            FileAttachment fileAttachment = new FileAttachment();
            fileAttachment.setPostId(postId);
            fileAttachment.setOriginalFilename(originalFilename);
            fileAttachment.setStoredFilename(storedFilename);
            fileAttachment.setFilePath(filePath.toString());
            fileAttachment.setFileSize(file.getSize());
            fileAttachment.setContentType(file.getContentType());

            fileAttachmentService.createFile(fileAttachment);
            System.out.println("파일 정보 DB 저장 완료. fileId: " + fileAttachment.getFileId());
            System.out.println("=== 파일 업로드 성공 ===");
            return ResponseEntity.ok(fileAttachment);
        } catch (IOException e) {
            System.out.println("ERROR: 파일 저장 실패 - " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일 업로드에 실패했습니다.");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId,
                                                  @RequestParam(required = false, defaultValue = "false") boolean inline) {
        FileAttachment fileAttachment = fileAttachmentService.getFileById(fileId);
        if (fileAttachment == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(fileAttachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = fileAttachment.getContentType();
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // 이미지 파일이면 inline으로 표시, 아니면 attachment로 다운로드
                String dispositionType = "attachment";
                if (contentType != null && contentType.startsWith("image/")) {
                    dispositionType = "inline";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                dispositionType + "; filename=\"" + fileAttachment.getOriginalFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        FileAttachment fileAttachment = fileAttachmentService.getFileById(fileId);
        if (fileAttachment == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일을 찾을 수 없습니다.");
            return ResponseEntity.notFound().build();
        }

        try {
            // Delete physical file
            Path filePath = Paths.get(fileAttachment.getFilePath());
            Files.deleteIfExists(filePath);

            // Delete database record
            fileAttachmentService.deleteFile(fileId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "파일이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일 삭제에 실패했습니다.");
            return ResponseEntity.status(500).body(error);
        }
    }
}
