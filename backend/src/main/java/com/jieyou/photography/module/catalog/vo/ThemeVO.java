package com.jieyou.photography.module.catalog.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主题响应，字段映射 t_theme；tags 转换为接口层数组（T-04）。
 */
public record ThemeVO(Long id,
                      String name,
                      String coverUrl,
                      String description,
                      List<String> tags,
                      Integer sort,
                      Integer status,
                      LocalDateTime createTime,
                      LocalDateTime updateTime) {
}
