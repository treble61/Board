package com.example.boards.service;

import com.example.boards.mapper.FileAttachmentMapper;
import com.example.boards.model.FileAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileAttachmentService {

    @Autowired
    private FileAttachmentMapper fileAttachmentMapper;

    public List<FileAttachment> getFilesByPostId(Long postId) {
        return fileAttachmentMapper.findByPostId(postId);
    }

    public FileAttachment getFileById(Long fileId) {
        return fileAttachmentMapper.findById(fileId);
    }

    public void createFile(FileAttachment file) {
        fileAttachmentMapper.insertFile(file);
    }

    public void deleteFile(Long fileId) {
        fileAttachmentMapper.deleteFile(fileId);
    }

    public void deleteFilesByPostId(Long postId) {
        fileAttachmentMapper.deleteByPostId(postId);
    }
}
