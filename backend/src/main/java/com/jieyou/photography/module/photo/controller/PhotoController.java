package com.jieyou.photography.module.photo.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.photo.service.PhotoService;
import com.jieyou.photography.module.photo.vo.PhotoVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 照片读取接口，依据 API 约定 5.5.2/5.5.3（USER 仅本人，ADMIN 全程可查）。
 */
@RestController
@RequestMapping("/api/orders/{orderId}/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public ApiResponse<List<PhotoVO>> list(@PathVariable Long orderId) {
        return ApiResponse.success(photoService.list(orderId));
    }

    @GetMapping("/{photoId}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long orderId,
                                            @PathVariable Long photoId) {
        return photoService.preview(orderId, photoId);
    }

    @GetMapping("/{photoId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long orderId,
                                             @PathVariable Long photoId) {
        return photoService.download(orderId, photoId);
    }
}
