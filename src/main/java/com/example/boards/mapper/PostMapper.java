package com.example.boards.mapper;

import com.example.boards.model.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PostMapper {
    List<Post> findAll(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("searchQuery") String searchQuery);
    int countAll(@Param("searchQuery") String searchQuery);
    Post findById(Long postId);
    void insertPost(Post post);
    void updatePost(Post post);
    void deletePost(Long postId);
    void incrementViewCount(Long postId);

    // Excel file operations
    void updateExcelFile(@Param("postId") Long postId,
                        @Param("excelFilename") String excelFilename,
                        @Param("excelStoredFilename") String excelStoredFilename,
                        @Param("excelFilePath") String excelFilePath,
                        @Param("excelFileSize") Long excelFileSize);
    void deleteExcelFile(@Param("postId") Long postId);
}
