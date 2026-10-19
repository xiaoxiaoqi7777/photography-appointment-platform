package com.jieyou.photography.module.feedback.vo;

import java.time.LocalDateTime;

/**
 * 反馈记录响应，依据 API 约定 5.6.2。
 */
public record FeedbackVO(Long id,
                         Integer round,
                         Integer photoRound,
                         String content,
                         LocalDateTime createTime) {
}
