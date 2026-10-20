package com.jieyou.photography.module.cover.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.cover.service.CoverService;
import com.jieyou.photography.module.cover.vo.CoverUploadVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端封面上传接口（ADMIN），用于主题/套餐封面。
 */
@RestController
@RequestMapping("/api/admin/covers")
public class AdminCoverController {

    private final CoverService coverService;

    public AdminCoverController(CoverService coverService) {
        this.coverService = coverService;
    }

    @PostMapping
    public ApiResponse<CoverUploadVO> upload(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ApiResponse.success(coverService.upload(file));
    }
}
