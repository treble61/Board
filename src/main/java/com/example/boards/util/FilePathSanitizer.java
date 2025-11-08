package com.example.boards.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 파일 경로 검증 및 정제를 위한 유틸리티 클래스
 * 경로 순회 공격(Path Traversal)을 방지합니다.
 */
public class FilePathSanitizer {

    /**
     * 파일명을 정제하고 안전한 저장 경로를 생성합니다.
     *
     * @param uploadDir 업로드 디렉토리
     * @param originalFilename 원본 파일명
     * @return 정제된 안전한 파일 경로
     * @throws SecurityException 경로 순회 시도가 감지된 경우
     */
    public static Path sanitizeFilePath(String uploadDir, String originalFilename) throws IOException {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new SecurityException("파일명이 비어있습니다.");
        }

        // 디렉토리 순회 시퀀스 제거 및 경로 구분자 제거
        String sanitized = originalFilename
            .replaceAll("\\.\\.", "")    // Remove .. sequences
            .replaceAll("[/\\\\]", "")   // Remove path separators
            .replaceAll("[\\x00-\\x1F]", ""); // Remove control characters

        if (sanitized.trim().isEmpty()) {
            throw new SecurityException("잘못된 파일명입니다.");
        }

        // UUID를 접두사로 추가하여 파일명 충돌 방지 및 보안 강화
        String storedFilename = UUID.randomUUID().toString() + "_" + sanitized;

        // 파일 경로 생성 및 정규화
        Path filePath = Paths.get(uploadDir, storedFilename).normalize();

        // 해석된 경로가 업로드 디렉토리 내에 있는지 검증
        Path uploadDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path absoluteFilePath = filePath.toAbsolutePath().normalize();

        if (!absoluteFilePath.startsWith(uploadDirPath)) {
            throw new SecurityException("잘못된 파일 경로: 디렉토리 순회 시도가 감지되었습니다.");
        }

        return filePath;
    }

    /**
     * 파일 확장자를 안전하게 추출합니다.
     *
     * @param filename 파일명
     * @return 파일 확장자 (점 포함) 또는 빈 문자열
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }

        return "";
    }

    /**
     * 파일명이 허용된 패턴인지 검증합니다.
     *
     * @param filename 검증할 파일명
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        // 파일명 길이 제한 (최대 255자)
        if (filename.length() > 255) {
            return false;
        }

        // 위험한 문자 패턴 검사
        String dangerous = "[\\\\/:<>\"'|?*\\x00-\\x1F]";
        return !filename.matches(".*" + dangerous + ".*");
    }
}
