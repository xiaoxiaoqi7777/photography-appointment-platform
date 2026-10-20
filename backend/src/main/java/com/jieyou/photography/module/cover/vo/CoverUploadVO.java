package com.jieyou.photography.module.cover.vo;

/**
 * 封面上传结果：只返回可公开读取的地址，不暴露磁盘路径与原始文件名。
 */
public record CoverUploadVO(String url) {
}
