package com.community.service.impl;

import com.community.common.BusinessException;
import com.community.config.SocialProperties;
import com.community.dto.SocialImageUploadResponse;
import com.community.service.SocialStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class SocialStorageServiceImpl implements SocialStorageService {

    private final SocialProperties properties;

    public SocialStorageServiceImpl(SocialProperties properties) {
        this.properties = properties;
    }

    @Override
    public SocialImageUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("操作失败");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("操作失败");
        }
        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("操作失败");
        }

        String dateFolder = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path targetDir = getUploadRoot().resolve(dateFolder).normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }

        String suffix = fileSuffix(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        Path targetFile = targetDir.resolve(fileName).normalize();
        try {
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }

        String relativePath = dateFolder + "/" + fileName;
        SocialImageUploadResponse response = new SocialImageUploadResponse();
        response.setPath(relativePath);
        response.setOriginalName(file.getOriginalFilename());
        response.setUrl("/api/social/file?path=" + URLEncoder.encode(relativePath, StandardCharsets.UTF_8));
        return response;
    }

    @Override
    public Resource loadImageAsResource(String path) {
        if (!StringUtils.hasText(path) || path.contains("..")) {
            throw new BusinessException(400, "请求参数不合法");
        }
        Path target = getUploadRoot().resolve(path).normalize();
        if (!target.startsWith(getUploadRoot())) {
            throw new BusinessException(400, "请求参数不合法");
        }
        if (!Files.exists(target) || !Files.isReadable(target)) {
            throw new BusinessException(404, "资源不存在");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }
    }

    private String fileSuffix(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".jpg";
        }
        int index = fileName.lastIndexOf(".");
        if (index < 0 || index == fileName.length() - 1) {
            return ".jpg";
        }
        String suffix = fileName.substring(index).toLowerCase(Locale.ROOT);
        if (suffix.length() > 10) {
            return ".jpg";
        }
        return suffix;
    }

    private Path getUploadRoot() {
        Path root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }
        return root;
    }
}

