package com.community.service;

import com.community.dto.SocialImageUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface SocialStorageService {

    SocialImageUploadResponse uploadImage(MultipartFile file);

    Resource loadImageAsResource(String path);
}

