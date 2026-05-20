package com.banny.lotsonote.service;

import com.banny.lotsonote.service.impl.LocalFileServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalFileServiceImplTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanUp() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
    }

    @Test
    void uploadImageShouldAcceptValidPng() {
        LocalFileServiceImpl fileService = createFileService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00}
        );

        String url = fileService.uploadImage(file);

        assertNotNull(url);
        assertTrue(url.startsWith("/images/"));
        assertEquals(1, tempDir.toFile().listFiles().length);
    }

    @Test
    void uploadImageShouldAcceptValidWebp() {
        LocalFileServiceImpl fileService = createFileService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.webp",
                "image/webp",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}
        );

        String url = fileService.uploadImage(file);

        assertNotNull(url);
        assertTrue(url.startsWith("/images/"));
    }

    @Test
    void uploadImageShouldCreateFileWhenUploadPathIsRelative() {
        LocalFileServiceImpl fileService = new LocalFileServiceImpl();
        String currentDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "uploadBasePath", "./relative-uploads");
        ReflectionTestUtils.setField(fileService, "urlPrefix", "/images");

        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "demo.png",
                    "image/png",
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00}
            );

            String url = fileService.uploadImage(file);

            assertNotNull(url);
            assertTrue(url.startsWith("/images/"));
            assertTrue(Files.exists(tempDir.resolve("relative-uploads")));
            assertEquals(1, tempDir.resolve("relative-uploads").toFile().listFiles().length);
        } finally {
            System.setProperty("user.dir", currentDir);
        }
    }

    @Test
    void uploadImageShouldRejectUnsupportedExtension() {
        LocalFileServiceImpl fileService = createFileService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.gif",
                "image/gif",
                new byte[]{0x47, 0x49, 0x46}
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileService.uploadImage(file));

        assertTrue(exception.getMessage().contains("只支持"));
    }

    @Test
    void uploadImageShouldRejectMismatchedSignature() {
        LocalFileServiceImpl fileService = createFileService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileService.uploadImage(file));

        assertEquals("文件内容与图片格式不匹配", exception.getMessage());
    }

    @Test
    void uploadImageShouldRejectUnsupportedContentType() {
        LocalFileServiceImpl fileService = createFileService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "application/octet-stream",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00}
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileService.uploadImage(file));

        assertEquals("图片内容类型不支持", exception.getMessage());
    }

    @Test
    void uploadImageShouldRejectOversizedFile() {
        LocalFileServiceImpl fileService = createFileService();
        byte[] content = new byte[10 * 1024 * 1024 + 1];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.jpg",
                "image/jpeg",
                content
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileService.uploadImage(file));

        assertEquals("图片大小不能超过 10MB", exception.getMessage());
    }

    @Test
    void uploadImageShouldRejectEmptyFile() {
        LocalFileServiceImpl fileService = createFileService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileService.uploadImage(file));

        assertEquals("上传的图片文件为空", exception.getMessage());
    }

    private LocalFileServiceImpl createFileService() {
        LocalFileServiceImpl fileService = new LocalFileServiceImpl();
        ReflectionTestUtils.setField(fileService, "uploadBasePath", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "urlPrefix", "/images");
        return fileService;
    }
}
