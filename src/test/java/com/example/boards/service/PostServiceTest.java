package com.example.boards.service;

import com.example.boards.mapper.PostMapper;
import com.example.boards.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private Post testPost;

    @BeforeEach
    void setUp() {
        testPost = new Post();
        testPost.setPostId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setAuthorId("testuser");
        testPost.setAuthorName("Test User");
        testPost.setIsNotice(false);
        testPost.setViewCount(0);
        testPost.setCreatedAt(new Date());
        testPost.setUpdatedAt(new Date());
    }

    @Test
    void testGetAllPosts() {
        // Given
        List<Post> mockPosts = Arrays.asList(testPost);
        when(postMapper.findAll(20, 0, null)).thenReturn(mockPosts);

        // When
        List<Post> result = postService.getAllPosts(20, 0, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(postMapper).findAll(20, 0, null);
    }

    @Test
    void testGetAllPostsWithSearchQuery() {
        // Given
        List<Post> mockPosts = Arrays.asList(testPost);
        when(postMapper.findAll(20, 0, "test")).thenReturn(mockPosts);

        // When
        List<Post> result = postService.getAllPosts(20, 0, "test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(postMapper).findAll(20, 0, "test");
    }

    @Test
    void testGetTotalCount() {
        // Given
        when(postMapper.countAll(null)).thenReturn(10);

        // When
        int result = postService.getTotalCount(null);

        // Then
        assertEquals(10, result);
        verify(postMapper).countAll(null);
    }

    @Test
    void testGetPostById() {
        // Given
        when(postMapper.findById(1L)).thenReturn(testPost);

        // When
        Post result = postService.getPostById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getPostId());
        assertEquals("Test Post", result.getTitle());
        verify(postMapper).incrementViewCount(1L);
        verify(postMapper).findById(1L);
    }

    @Test
    void testCreatePost() {
        // Given
        Post newPost = new Post();
        newPost.setTitle("New Post");
        newPost.setContent("New Content");
        newPost.setAuthorId("testuser");

        // When
        postService.createPost(newPost);

        // Then
        verify(postMapper).insertPost(newPost);
    }

    @Test
    void testUpdatePost() {
        // Given
        testPost.setTitle("Updated Post");
        testPost.setContent("Updated Content");

        // When
        postService.updatePost(testPost);

        // Then
        verify(postMapper).updatePost(testPost);
    }

    @Test
    void testDeletePost() {
        // When
        postService.deletePost(1L);

        // Then
        verify(postMapper).deletePost(1L);
    }
}

