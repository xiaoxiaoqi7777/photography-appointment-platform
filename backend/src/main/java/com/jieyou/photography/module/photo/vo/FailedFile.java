package com.jieyou.photography.module.photo.vo;

/**
 * 批量上传中失败的文件（API 约定 5.5.1）。
 */
public record FailedFile(String name, String reason) {
}
