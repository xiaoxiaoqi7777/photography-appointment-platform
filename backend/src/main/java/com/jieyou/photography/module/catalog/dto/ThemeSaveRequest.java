package com.jieyou.photography.module.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 主题新增/编辑请求，依据 API 约定 5.2.2/5.2.3：
 * 新增时 name 必填（服务层校验）；编辑时仅提交需要修改的字段。
 * tags 接口层为数组，持久层逗号分隔（T-04）。
 */
public record ThemeSaveRequest(@Size(max = 50, message = "主题名称最长 50 个字符")
                               String name,
                               @Size(max = 255, message = "封面地址最长 255 个字符")
                               String coverUrl,
                               @Size(max = 500, message = "主题描述最长 500 个字符")
                               String description,
                               @Size(max = 20, message = "标签最多 20 个")
                               List<@Size(max = 30, message = "单个标签最长 30 个字符") String> tags,
                               Integer sort,
                               @Min(value = 0, message = "状态只能为 0 或 1")
                               @Max(value = 1, message = "状态只能为 0 或 1")
                               Integer status) {
}
