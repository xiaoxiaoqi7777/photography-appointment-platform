package com.jieyou.photography.module.photo.vo;

import java.util.List;

/**
 * 批量上传结果：部分失败仍返回 HTTP 200，成功与失败分开列出。
 */
public record UploadResult(List<String> success, List<FailedFile> failed) {
}
