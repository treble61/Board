package com.example.boards.model;

import lombok.Data;
import java.util.Date;

@Data
public class FileAttachment {
    private Long fileId;
    private Long postId;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private Date createdAt;
}
