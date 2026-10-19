package com.jieyou.photography.module.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交修改意见请求，依据 API 约定 5.6.1：content 必填且 ≤500 字。
 */
public record FeedbackRequest(
        @NotBlank(message = "修改意见不能为空")
        @Size(max = 500, message = "修改意见不能超过 500 字")
        String content) {
}
