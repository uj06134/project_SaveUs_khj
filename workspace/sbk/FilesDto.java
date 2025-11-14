package com.example.Ex02.Dto;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FilesDto {
    private Long id;
    private Long postId;
    private String originalFilename;
    private String storedFilepath;
    private Long fileSize;
    private Timestamp createdAt;
}
