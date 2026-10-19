package com.jieyou.photography.module.catalog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jieyou.photography.common.api.PageQuery;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.catalog.dto.ThemeSaveRequest;
import com.jieyou.photography.module.catalog.entity.Theme;
import com.jieyou.photography.module.catalog.mapper.ThemeMapper;
import com.jieyou.photography.module.catalog.vo.ThemeVO;
import com.jieyou.photography.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 主题业务逻辑，依据 API 约定 5.2：
 * 管理端可查看全部并维护上下架；用户端仅可见上架主题（下架按 40400 处理）。
 */
@Service
public class ThemeService {

    private static final int STATUS_ON = 1;
    private static final int STATUS_OFF = 0;
    private static final int MAX_TAGS_LENGTH = 200;

    private final ThemeMapper themeMapper;

    public ThemeService(ThemeMapper themeMapper) {
        this.themeMapper = themeMapper;
    }

    /**
     * 管理端主题分页列表（GET /api/admin/themes），支持状态筛选。
     */
    public PageResult<ThemeVO> adminList(Integer status, Integer page, Integer pageSize) {
        validateStatusFilter(status);
        PageQuery query = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<Theme>()
                .eq(status != null, Theme::getStatus, status)
                .orderByDesc(Theme::getSort)
                .orderByDesc(Theme::getId);
        return selectPage(query, wrapper);
    }

    /**
     * 用户端主题分页列表（GET /api/themes），仅上架，按 sort 倒序。
     */
    public PageResult<ThemeVO> publicList(Integer page, Integer pageSize) {
        PageQuery query = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<Theme>()
                .eq(Theme::getStatus, STATUS_ON)
                .orderByDesc(Theme::getSort)
                .orderByDesc(Theme::getId);
        return selectPage(query, wrapper);
    }

    /**
     * 主题详情（GET /api/themes/{id}）：下架主题仅管理员可见。
     */
    public ThemeVO detail(Long id) {
        Theme theme = requireTheme(id);
        if (isOff(theme) && !SecurityUtils.isAdmin()) {
            throw notFound();
        }
        return toVO(theme);
    }

    /**
     * 新增主题（POST /api/admin/themes）。
     */
    public ThemeVO create(ThemeSaveRequest request) {
        Theme theme = new Theme();
        theme.setName(requireName(request.name()));
        theme.setCoverUrl(trimToNull(request.coverUrl()));
        theme.setDescription(trimToNull(request.description()));
        theme.setTags(toDbTags(request.tags()));
        theme.setSort(request.sort() == null ? 0 : request.sort());
        theme.setStatus(request.status() == null ? STATUS_ON : validateStatus(request.status()));
        themeMapper.insert(theme);
        return toVO(themeMapper.selectById(theme.getId()));
    }

    /**
     * 编辑主题（PUT /api/admin/themes/{id}）：仅更新请求中提供的字段。
     */
    public ThemeVO update(Long id, ThemeSaveRequest request) {
        requireTheme(id);
        LambdaUpdateWrapper<Theme> update = new LambdaUpdateWrapper<Theme>().eq(Theme::getId, id);
        boolean changed = false;
        if (request.name() != null) {
            update.set(Theme::getName, requireName(request.name()));
            changed = true;
        }
        if (request.coverUrl() != null) {
            update.set(Theme::getCoverUrl, trimToNull(request.coverUrl()));
            changed = true;
        }
        if (request.description() != null) {
            update.set(Theme::getDescription, trimToNull(request.description()));
            changed = true;
        }
        if (request.tags() != null) {
            update.set(Theme::getTags, toDbTags(request.tags()));
            changed = true;
        }
        if (request.sort() != null) {
            update.set(Theme::getSort, request.sort());
            changed = true;
        }
        if (request.status() != null) {
            update.set(Theme::getStatus, validateStatus(request.status()));
            changed = true;
        }
        if (!changed) {
            throw new BizException(ResultCode.PARAM_INVALID, "没有需要更新的字段");
        }
        themeMapper.update(null, update);
        return toVO(themeMapper.selectById(id));
    }

    /**
     * 主题上下架（PATCH /api/admin/themes/{id}/status），仅软下架，无物理删除（T-01）。
     */
    public ThemeVO updateStatus(Long id, Integer status) {
        requireTheme(id);
        themeMapper.update(null, new LambdaUpdateWrapper<Theme>()
                .eq(Theme::getId, id)
                .set(Theme::getStatus, validateStatus(status)));
        return toVO(themeMapper.selectById(id));
    }

    private PageResult<ThemeVO> selectPage(PageQuery query, LambdaQueryWrapper<Theme> wrapper) {
        Page<Theme> result = themeMapper.selectPage(new Page<>(query.page(), query.pageSize()), wrapper);
        List<ThemeVO> list = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), list);
    }

    private Theme requireTheme(Long id) {
        Theme theme = themeMapper.selectById(id);
        if (theme == null) {
            throw notFound();
        }
        return theme;
    }

    private static boolean isOff(Theme theme) {
        return theme.getStatus() == null || theme.getStatus() != STATUS_ON;
    }

    private static BizException notFound() {
        return new BizException(ResultCode.NOT_FOUND, "主题不存在");
    }

    private static String requireName(String name) {
        String trimmed = trimToNull(name);
        if (trimmed == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "主题名称不能为空");
        }
        return trimmed;
    }

    private static int validateStatus(Integer status) {
        if (status == null || (status != STATUS_ON && status != STATUS_OFF)) {
            throw new BizException(ResultCode.PARAM_INVALID, "状态只能为 0 或 1");
        }
        return status;
    }

    private static void validateStatusFilter(Integer status) {
        if (status != null) {
            validateStatus(status);
        }
    }

    /**
     * 接口层数组 → DB 逗号分隔字符串（T-04），空数组清空为 NULL。
     */
    private static String toDbTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains(",")) {
                throw new BizException(ResultCode.PARAM_INVALID, "标签不能包含逗号");
            }
            normalized.add(trimmed);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        String joined = String.join(",", normalized);
        if (joined.length() > MAX_TAGS_LENGTH) {
            throw new BizException(ResultCode.PARAM_INVALID, "标签总长度不能超过 200 个字符");
        }
        return joined;
    }

    private static List<String> fromDbTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ThemeVO toVO(Theme theme) {
        return new ThemeVO(theme.getId(), theme.getName(), theme.getCoverUrl(), theme.getDescription(),
                fromDbTags(theme.getTags()), theme.getSort(), theme.getStatus(),
                theme.getCreateTime(), theme.getUpdateTime());
    }
}
