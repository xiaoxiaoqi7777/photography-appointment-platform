package com.jieyou.photography.module.catalog.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.module.catalog.dto.StatusRequest;
import com.jieyou.photography.module.catalog.dto.ThemeSaveRequest;
import com.jieyou.photography.module.catalog.service.ThemeService;
import com.jieyou.photography.module.catalog.vo.ThemeVO;
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
 * 管理端主题接口，依据 API 约定 5.2.1~5.2.4。
 */
@RestController
@RequestMapping("/api/admin/themes")
public class AdminThemeController {

    private final ThemeService themeService;

    public AdminThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public ApiResponse<PageResult<ThemeVO>> list(@RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(themeService.adminList(status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ThemeVO> create(@Valid @RequestBody ThemeSaveRequest request) {
        return ApiResponse.success(themeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ThemeVO> update(@PathVariable Long id, @Valid @RequestBody ThemeSaveRequest request) {
        return ApiResponse.success(themeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ThemeVO> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(themeService.updateStatus(id, request.status()));
    }
}
