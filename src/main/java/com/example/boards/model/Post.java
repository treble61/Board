package com.example.boards.model;

import lombok.Data;
import java.util.Date;

@Data
public class Post {
    private Long postId;
    private String title;
    private String content;
    private String authorId;
    private String authorName;
    private Boolean isNotice;
    private Integer viewCount;
    private Date createdAt;
    private Date updatedAt;

    // 엑셀 파일 정보
    private String excelFilename;
    private String excelStoredFilename;
    private String excelFilePath;
    private Long excelFileSize;

    // 추가 정보
    private Integer commentCount;
    private Integer fileCount;
}
