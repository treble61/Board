package com.example.boards.service;

import com.example.boards.mapper.PostMapper;
import com.example.boards.model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    /**
     * LIKE 패턴의 특수문자(%,_)를 이스케이프 처리
     */
    private String escapeLikePattern(String searchQuery) {
        if (searchQuery == null || searchQuery.isEmpty()) {
            return searchQuery;
        }
        // % 와 _ 를 이스케이프 처리
        return searchQuery.replace("\\", "\\\\")
                         .replace("%", "\\%")
                         .replace("_", "\\_");
    }

    public List<Post> getAllPosts(Integer limit, Integer offset, String searchQuery) {
        String escapedQuery = escapeLikePattern(searchQuery);
        return postMapper.findAll(limit, offset, escapedQuery);
    }

    public int getTotalCount(String searchQuery) {
        String escapedQuery = escapeLikePattern(searchQuery);
        return postMapper.countAll(escapedQuery);
    }

    @Transactional
    public Post getPostById(Long postId) {
        postMapper.incrementViewCount(postId);
        return postMapper.findById(postId);
    }

    public void createPost(Post post) {
        postMapper.insertPost(post);
    }

    public void updatePost(Post post) {
        postMapper.updatePost(post);
    }

    public void deletePost(Long postId) {
        postMapper.deletePost(postId);
    }

    public void updateExcelFile(Long postId, String excelFilename, String excelStoredFilename,
                               String excelFilePath, Long excelFileSize) {
        postMapper.updateExcelFile(postId, excelFilename, excelStoredFilename, excelFilePath, excelFileSize);
    }

    public void deleteExcelFile(Long postId) {
        postMapper.deleteExcelFile(postId);
    }
}
