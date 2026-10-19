package com.jieyou.photography.module.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 上下架请求，依据 API 约定 5.2.4/5.3.4：{ "status": 0 } 下架 / { "status": 1 } 上架。
 */
public record StatusRequest(@NotNull(message = "状态不能为空")
                            @Min(value = 0, message = "状态只能为 0 或 1")
                            @Max(value = 1, message = "状态只能为 0 或 1")
                            Integer status) {
}
