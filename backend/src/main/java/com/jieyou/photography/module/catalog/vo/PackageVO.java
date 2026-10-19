package com.jieyou.photography.module.catalog.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐响应，字段映射 t_package。
 */
public record PackageVO(Long id,
                        Long themeId,
                        String name,
                        BigDecimal price,
                        Integer retouchCount,
                        String duration,
                        String includes,
                        String coverUrl,
                        Integer sort,
                        Integer status,
                        LocalDateTime createTime,
                        LocalDateTime updateTime) {
}
