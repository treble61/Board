package com.example.boards.controller;

import com.example.boards.model.Comment;
import com.example.boards.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPostId(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody Comment comment, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        comment.setAuthorId(userId);
        commentService.createComment(comment);
        return ResponseEntity.ok(comment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId, @RequestBody Comment comment, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        Comment existingComment = commentService.getCommentById(commentId);
        if (!existingComment.getAuthorId().equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "수정 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        comment.setCommentId(commentId);
        comment.setAuthorId(userId);
        commentService.updateComment(comment);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        Comment existingComment = commentService.getCommentById(commentId);
        if (!existingComment.getAuthorId().equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "삭제 권한이 없습니다.");
            return ResponseEntity.status(403).body(error);
        }

        commentService.deleteComment(commentId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "댓글이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
}
