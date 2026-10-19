package com.jieyou.photography.module.catalog.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.catalog.service.PackageService;
import com.jieyou.photography.module.catalog.vo.PackageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端套餐接口，依据 API 约定 5.3.5/5.3.6（公开访问，仅上架套餐）。
 */
@RestController
public class PackageController {

    private final PackageService packageService;

    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping("/api/themes/{themeId}/packages")
    public ApiResponse<List<PackageVO>> listByTheme(@PathVariable Long themeId) {
        return ApiResponse.success(packageService.userListByTheme(themeId));
    }

    @GetMapping("/api/packages/{id}")
    public ApiResponse<PackageVO> detail(@PathVariable Long id) {
        return ApiResponse.success(packageService.detail(id));
    }
}
