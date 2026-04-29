package com.banny.lotsonote.service;

import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.upload.UploadImageData;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    /**
     * 上传图片
     */
    ApiResponse<UploadImageData> uploadImage(MultipartFile file);
}
