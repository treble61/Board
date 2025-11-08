package com.example.boards.mapper;

import com.example.boards.model.FileAttachment;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface FileAttachmentMapper {
    List<FileAttachment> findByPostId(Long postId);
    FileAttachment findById(Long fileId);
    void insertFile(FileAttachment file);
    void deleteFile(Long fileId);
    void deleteByPostId(Long postId);
}
