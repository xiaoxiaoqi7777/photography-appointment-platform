package com.jieyou.photography.module.catalog.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.module.catalog.dto.PackageSaveRequest;
import com.jieyou.photography.module.catalog.dto.StatusRequest;
import com.jieyou.photography.module.catalog.service.PackageService;
import com.jieyou.photography.module.catalog.vo.PackageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端套餐接口，依据 API 约定 5.3.1~5.3.4。
 */
@RestController
@RequestMapping("/api/admin/packages")
public class AdminPackageController {

    private final PackageService packageService;

    public AdminPackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    public ApiResponse<PageResult<PackageVO>> list(@RequestParam(required = false) Long themeId,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(packageService.adminList(themeId, status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<PackageVO> create(@Valid @RequestBody PackageSaveRequest request) {
        PackageService.SaveResult result = packageService.create(request);
        return ApiResponse.success(result.packageVO(), result.message());
    }

    @PutMapping("/{id}")
    public ApiResponse<PackageVO> update(@PathVariable Long id, @Valid @RequestBody PackageSaveRequest request) {
        PackageService.SaveResult result = packageService.update(id, request);
        return ApiResponse.success(result.packageVO(), result.message());
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PackageVO> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(packageService.updateStatus(id, request.status()));
    }
}
