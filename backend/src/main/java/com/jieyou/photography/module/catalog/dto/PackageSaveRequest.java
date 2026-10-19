package com.jieyou.photography.module.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 套餐新增/编辑请求，依据 API 约定 5.3.2/5.3.3：
 * 新增时 themeId/name/price/retouchCount 必填（服务层校验）；编辑时仅提交需要修改的字段。
 */
public record PackageSaveRequest(Long themeId,
                                 @Size(max = 50, message = "套餐名称最长 50 个字符")
                                 String name,
                                 @DecimalMin(value = "0.01", message = "价格必须大于 0")
                                 @Digits(integer = 8, fraction = 2, message = "价格最多两位小数")
                                 BigDecimal price,
                                 Integer retouchCount,
                                 @Size(max = 50, message = "拍摄时长最长 50 个字符")
                                 String duration,
                                 @Size(max = 500, message = "包含内容最长 500 个字符")
                                 String includes,
                                 @Size(max = 255, message = "封面地址最长 255 个字符")
                                 String coverUrl,
                                 Integer sort,
                                 @Min(value = 0, message = "状态只能为 0 或 1")
                                 @Max(value = 1, message = "状态只能为 0 或 1")
                                 Integer status) {
}
