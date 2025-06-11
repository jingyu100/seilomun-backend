package com.yju.team2.seilomun.domain.upload.controller;

import com.yju.team2.seilomun.domain.upload.service.AWSS3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
public class AWSS3Controller {

    private final AWSS3UploadService awsS3UploadService;

    @PostMapping
    public ResponseEntity<String> uploadFile(MultipartFile file) {
        return ResponseEntity.ok(awsS3UploadService.uploadFile(file));
    }

    @PostMapping("/list")
    public ResponseEntity<List<String>> uploadFiles(List<MultipartFile> multipartFiles) {
        return ResponseEntity.ok(awsS3UploadService.uploadFiles(multipartFiles));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteFile(@RequestParam String fileName) {
        awsS3UploadService.deleteFile(fileName);
        return ResponseEntity.ok(fileName);
    }

}
