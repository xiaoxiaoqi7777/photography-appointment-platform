package com.jieyou.photography.module.photo.vo;

/**
 * 照片列表项（API 约定 5.5.2）：不暴露原图路径，仅提供鉴权预览入口。
 */
public record PhotoVO(Long id, String originalName, Long fileSize, Integer uploadRound, String previewUrl) {
}
