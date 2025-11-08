package com.example.boards.model;

import lombok.Data;
import java.util.Date;

@Data
public class Comment {
    private Long commentId;
    private Long postId;
    private String authorId;
    private String authorName;
    private String content;
    private Date createdAt;
    private Date updatedAt;
}
