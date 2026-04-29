package com.banny.lotsonote.service;

import com.banny.lotsonote.aspect.NeedLoginAspect;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.upload.UploadImageData;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UploadServiceImplTest {

    @Mock
    private FileService fileService;

    @Test
    void uploadImageShouldReturnUnauthenticatedErrorWhenNotLoggedIn() {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(false);

        UploadService uploadService = createProxy(requestScopeData);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        ApiResponse<UploadImageData> response = uploadService.uploadImage(file);

        assertEquals(400, response.getCode());
        assertEquals("用户未登录", response.getMessage());
        verify(fileService, never()).uploadImage(any());
    }

    @Test
    void uploadImageShouldReturnUrlWithNewDtoWhenLoggedIn() {
        RequestScopeData requestScopeData = new RequestScopeData();
        requestScopeData.setLogin(true);
        requestScopeData.setUserId(100L);

        UploadService uploadService = createProxy(requestScopeData);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(fileService.uploadImage(file)).thenReturn("/images/demo.png");

        ApiResponse<UploadImageData> response = uploadService.uploadImage(file);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("/images/demo.png", response.getData().getUrl());
        verify(fileService).uploadImage(file);
    }

    private UploadService createProxy(RequestScopeData requestScopeData) {
        UploadServiceImpl target = new UploadServiceImpl();
        ReflectionTestUtils.setField(target, "fileService", fileService);

        NeedLoginAspect aspect = new NeedLoginAspect();
        ReflectionTestUtils.setField(aspect, "requestScopeData", requestScopeData);

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }
}
