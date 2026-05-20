package com.banny.lotsonote.service.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.banny.lotsonote.service.FileService;

@Service
public class LocalFileServiceImpl implements FileService {
    private static final int IMAGE_SIGNATURE_LENGTH = 12;
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES
            = Arrays.asList("image/jpeg", "image/png", "image/webp");

    @Value("${upload.path}")
    private String uploadBasePath;

    @Value("${upload.url-prefix}")
    private String urlPrefix;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS
            = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的图片文件为空");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("图片文件名无效");
        }

        String lowerCaseExtension = originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(lowerCaseExtension)) {
            throw new IllegalArgumentException(
                    "只支持 " + ALLOWED_IMAGE_EXTENSIONS + " 格式图片");
        }

        validateImageContentType(file, lowerCaseExtension);
        validateImageSignature(file, lowerCaseExtension);
        return doUpload(file);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        return doUpload(file);
    }

    private String doUpload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名不合法");
        }

        String fileExtension = originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();
        String newFileName = UUID.randomUUID() + fileExtension;

        Path uploadDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDir, e);
        }

        Path destFile = uploadDir.resolve(newFileName);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败: " + e.getMessage(), e);
        }

        return urlPrefix + "/" + newFileName;
    }

    private void validateImageSignature(MultipartFile file, String extension) {
        byte[] signature = new byte[IMAGE_SIGNATURE_LENGTH];
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            int read = inputStream.read(signature);
            if (read < 0) {
                throw new IllegalArgumentException("上传的图片文件为空");
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败: " + e.getMessage(), e);
        }

        boolean matched = switch (extension) {
            case ".jpg", ".jpeg" -> isJpeg(signature);
            case ".png" -> isPng(signature);
            case ".webp" -> isWebp(signature);
            default -> false;
        };

        if (!matched) {
            throw new IllegalArgumentException("文件内容与图片格式不匹配");
        }
    }

    private void validateImageContentType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("图片内容类型不支持");
        }

        boolean matched = switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg".equals(contentType);
            case ".png" -> "image/png".equals(contentType);
            case ".webp" -> "image/webp".equals(contentType);
            default -> false;
        };

        if (!matched) {
            throw new IllegalArgumentException("文件内容与图片格式不匹配");
        }
    }

    private boolean isJpeg(byte[] signature) {
        return signature.length >= 3
                && (signature[0] & 0xFF) == 0xFF
                && (signature[1] & 0xFF) == 0xD8
                && (signature[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] signature) {
        return signature.length >= 8
                && (signature[0] & 0xFF) == 0x89
                && signature[1] == 0x50
                && signature[2] == 0x4E
                && signature[3] == 0x47
                && signature[4] == 0x0D
                && signature[5] == 0x0A
                && signature[6] == 0x1A
                && signature[7] == 0x0A;
    }

    private boolean isWebp(byte[] signature) {
        return signature.length >= 12
                && signature[0] == 0x52
                && signature[1] == 0x49
                && signature[2] == 0x46
                && signature[3] == 0x46
                && signature[8] == 0x57
                && signature[9] == 0x45
                && signature[10] == 0x42
                && signature[11] == 0x50;
    }
}
