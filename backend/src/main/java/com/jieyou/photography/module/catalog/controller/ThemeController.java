package com.jieyou.photography.module.catalog.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.module.catalog.service.ThemeService;
import com.jieyou.photography.module.catalog.vo.ThemeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端主题接口，依据 API 约定 5.2.5/5.2.6（公开访问，仅上架主题）。
 */
@RestController
@RequestMapping("/api/themes")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public ApiResponse<PageResult<ThemeVO>> list(@RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(themeService.publicList(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ThemeVO> detail(@PathVariable Long id) {
        return ApiResponse.success(themeService.detail(id));
    }
}
