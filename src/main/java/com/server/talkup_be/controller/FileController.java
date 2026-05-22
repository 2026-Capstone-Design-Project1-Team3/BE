package com.server.talkup_be.controller;

import com.server.talkup_be.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.server.talkup_be.dto.FileDto;

@RestController
@RequestMapping("/files")
public class FileController {

    private final S3Service s3Service;

    public FileController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    // s3 파일 업로드 링크 반환
    @GetMapping("/presignedUrl/{fileName}")
    public ResponseEntity<FileDto> getPresignedUrl(@PathVariable("fileName") String fileName) {

        FileDto response = s3Service.getPresignedUploadUrl(fileName);

        return ResponseEntity.ok(response);
    }
}