package com.jieyou.photography.module.photo.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.photo.service.PhotoService;
import com.jieyou.photography.module.photo.vo.UploadResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理端照片上传接口，依据 API 约定 5.5.1（ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/orders/{orderId}/photos")
public class AdminPhotoController {

    private final PhotoService photoService;

    public AdminPhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping
    public ApiResponse<UploadResult> upload(@PathVariable Long orderId,
                                            @RequestParam(value = "files", required = false)
                                            List<MultipartFile> files) {
        return ApiResponse.success(photoService.upload(orderId, files));
    }
}
