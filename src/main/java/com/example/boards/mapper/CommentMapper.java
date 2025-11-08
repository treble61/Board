package com.example.boards.mapper;

import com.example.boards.model.Comment;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> findByPostId(Long postId);
    Comment findById(Long commentId);
    void insertComment(Comment comment);
    void updateComment(Comment comment);
    void deleteComment(Long commentId);
    int countByPostId(Long postId);
}
