package com.jieyou.photography.module.cover.controller;

import com.jieyou.photography.module.cover.service.CoverService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 封面图片公开读取接口：封面为营销素材，可匿名访问；只接受服务端生成的文件名。
 */
@RestController
@RequestMapping("/api/covers")
public class CoverController {

    private final CoverService coverService;

    public CoverController(CoverService coverService) {
        this.coverService = coverService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> read(@PathVariable String filename) {
        return coverService.read(filename);
    }
}
