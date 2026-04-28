package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.vo.upload.ImageVO;
import com.banny.lotsonote.service.FileService;
import com.banny.lotsonote.service.UploadService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadServiceImpl implements UploadService {

    @Autowired
    FileService fileService;

    @Override
    public ApiResponse<ImageVO> uploadImage(MultipartFile file) {
        String url = fileService.uploadImage(file);
        ImageVO imageVO = new ImageVO();
        imageVO.setUrl(url);
        return ApiResponseUtil.success("上传成功", imageVO);
    }
}