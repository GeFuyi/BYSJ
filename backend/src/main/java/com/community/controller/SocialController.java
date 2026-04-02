package com.community.controller;

import com.community.common.ApiResponse;
import com.community.dto.SocialImageUploadResponse;
import com.community.service.SocialStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialStorageService storageService;

    public SocialController(SocialStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload-image")
    public ApiResponse<SocialImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(storageService.uploadImage(file));
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> getImage(@RequestParam("path") String path) {
        Resource resource = storageService.loadImageAsResource(path);
        String contentType = "application/octet-stream";
        try {
            contentType = resource.getFile() == null
                    ? contentType
                    : java.nio.file.Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ignored) {
            // ignore and use default content type
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                .body(resource);
    }
}

