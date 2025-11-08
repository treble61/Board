package com.example.boards.service;

import com.example.boards.mapper.CommentMapper;
import com.example.boards.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentMapper.findByPostId(postId);
    }

    public Comment getCommentById(Long commentId) {
        return commentMapper.findById(commentId);
    }

    public void createComment(Comment comment) {
        commentMapper.insertComment(comment);
    }

    public void updateComment(Comment comment) {
        commentMapper.updateComment(comment);
    }

    public void deleteComment(Long commentId) {
        commentMapper.deleteComment(commentId);
    }

    public int getCommentCount(Long postId) {
        return commentMapper.countByPostId(postId);
    }
}
