package com.jieyou.photography.module.catalog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jieyou.photography.common.api.PageQuery;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.catalog.dto.PackageSaveRequest;
import com.jieyou.photography.module.catalog.entity.Package;
import com.jieyou.photography.module.catalog.entity.Theme;
import com.jieyou.photography.module.catalog.mapper.PackageMapper;
import com.jieyou.photography.module.catalog.mapper.ThemeMapper;
import com.jieyou.photography.module.catalog.vo.PackageVO;
import com.jieyou.photography.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 套餐业务逻辑，依据 API 约定 5.3：
 * 管理端维护全部套餐；用户端仅可见上架套餐（下架按 40400 处理）。
 * 关联主题下架时允许保存，但返回提示信息。
 */
@Service
public class PackageService {

    private static final int STATUS_ON = 1;
    private static final int STATUS_OFF = 0;

    private final PackageMapper packageMapper;
    private final ThemeMapper themeMapper;

    public PackageService(PackageMapper packageMapper, ThemeMapper themeMapper) {
        this.packageMapper = packageMapper;
        this.themeMapper = themeMapper;
    }

    /**
     * 管理端套餐分页列表（GET /api/admin/packages），支持按主题、状态筛选。
     */
    public PageResult<PackageVO> adminList(Long themeId, Integer status, Integer page, Integer pageSize) {
        validateStatusFilter(status);
        PageQuery query = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<Package> wrapper = new LambdaQueryWrapper<Package>()
                .eq(themeId != null, Package::getThemeId, themeId)
                .eq(status != null, Package::getStatus, status)
                .orderByDesc(Package::getSort)
                .orderByDesc(Package::getId);
        return selectPage(query, wrapper);
    }

    /**
     * 用户端按主题查询套餐（GET /api/themes/{themeId}/packages）：仅上架套餐，按 sort 倒序。
     */
    public List<PackageVO> userListByTheme(Long themeId) {
        Theme theme = requireTheme(themeId);
        if (isOff(theme) && !SecurityUtils.isAdmin()) {
            throw new BizException(ResultCode.NOT_FOUND, "主题不存在");
        }
        return packageMapper.selectList(new LambdaQueryWrapper<Package>()
                        .eq(Package::getThemeId, themeId)
                        .eq(Package::getStatus, STATUS_ON)
                        .orderByDesc(Package::getSort)
                        .orderByDesc(Package::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 套餐详情（GET /api/packages/{id}）：下架套餐仅管理员可见。
     */
    public PackageVO detail(Long id) {
        Package pkg = requirePackage(id);
        if (isOff(pkg) && !SecurityUtils.isAdmin()) {
            throw new BizException(ResultCode.NOT_FOUND, "套餐不存在");
        }
        Theme theme = themeMapper.selectById(pkg.getThemeId());
        if (theme == null || (isOff(theme) && !SecurityUtils.isAdmin())) {
            throw new BizException(ResultCode.NOT_FOUND, "套餐不存在");
        }
        return toVO(pkg);
    }

    /**
     * 新增套餐（POST /api/admin/packages），依据 5.3.2 校验必填项与价格/精修张数。
     */
    public SaveResult create(PackageSaveRequest request) {
        if (request.themeId() == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "所属主题不能为空");
        }
        Theme theme = requireTheme(request.themeId());
        Package pkg = new Package();
        pkg.setThemeId(theme.getId());
        pkg.setName(requireName(request.name()));
        pkg.setPrice(requirePrice(request.price()));
        pkg.setRetouchCount(requireRetouchCount(request.retouchCount()));
        pkg.setDuration(trimToNull(request.duration()));
        pkg.setIncludes(trimToNull(request.includes()));
        pkg.setCoverUrl(trimToNull(request.coverUrl()));
        pkg.setSort(request.sort() == null ? 0 : request.sort());
        pkg.setStatus(request.status() == null ? STATUS_ON : validateStatus(request.status()));
        packageMapper.insert(pkg);
        return new SaveResult(toVO(packageMapper.selectById(pkg.getId())), themeSaveMessage(theme));
    }

    /**
     * 编辑套餐（PUT /api/admin/packages/{id}）：仅更新请求中提供的字段。
     */
    public SaveResult update(Long id, PackageSaveRequest request) {
        Package existing = requirePackage(id);
        LambdaUpdateWrapper<Package> update = new LambdaUpdateWrapper<Package>().eq(Package::getId, id);
        Theme theme = requireTheme(existing.getThemeId());
        boolean changed = false;
        if (request.themeId() != null) {
            theme = requireTheme(request.themeId());
            update.set(Package::getThemeId, theme.getId());
            changed = true;
        }
        if (request.name() != null) {
            update.set(Package::getName, requireName(request.name()));
            changed = true;
        }
        if (request.price() != null) {
            update.set(Package::getPrice, requirePrice(request.price()));
            changed = true;
        }
        if (request.retouchCount() != null) {
            update.set(Package::getRetouchCount, requireRetouchCount(request.retouchCount()));
            changed = true;
        }
        if (request.duration() != null) {
            update.set(Package::getDuration, trimToNull(request.duration()));
            changed = true;
        }
        if (request.includes() != null) {
            update.set(Package::getIncludes, trimToNull(request.includes()));
            changed = true;
        }
        if (request.coverUrl() != null) {
            update.set(Package::getCoverUrl, trimToNull(request.coverUrl()));
            changed = true;
        }
        if (request.sort() != null) {
            update.set(Package::getSort, request.sort());
            changed = true;
        }
        if (request.status() != null) {
            update.set(Package::getStatus, validateStatus(request.status()));
            changed = true;
        }
        if (!changed) {
            throw new BizException(ResultCode.PARAM_INVALID, "没有需要更新的字段");
        }
        packageMapper.update(null, update);
        return new SaveResult(toVO(packageMapper.selectById(id)), themeSaveMessage(theme));
    }

    /**
     * 套餐上下架（PATCH /api/admin/packages/{id}/status），仅软下架，无物理删除（T-01）。
     */
    public PackageVO updateStatus(Long id, Integer status) {
        requirePackage(id);
        packageMapper.update(null, new LambdaUpdateWrapper<Package>()
                .eq(Package::getId, id)
                .set(Package::getStatus, validateStatus(status)));
        return toVO(packageMapper.selectById(id));
    }

    private PageResult<PackageVO> selectPage(PageQuery query, LambdaQueryWrapper<Package> wrapper) {
        Page<Package> result = packageMapper.selectPage(new Page<>(query.page(), query.pageSize()), wrapper);
        List<PackageVO> list = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), list);
    }

    private Theme requireTheme(Long themeId) {
        Theme theme = themeId == null ? null : themeMapper.selectById(themeId);
        if (theme == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "关联主题不存在");
        }
        return theme;
    }

    private Package requirePackage(Long id) {
        Package pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BizException(ResultCode.NOT_FOUND, "套餐不存在");
        }
        return pkg;
    }

    private static boolean isOff(Theme theme) {
        return theme.getStatus() == null || theme.getStatus() != STATUS_ON;
    }

    private static boolean isOff(Package pkg) {
        return pkg.getStatus() == null || pkg.getStatus() != STATUS_ON;
    }

    /**
     * 关联主题已下架时允许保存，但向管理员返回提示（5.3.2 A-5）。
     */
    private static String themeSaveMessage(Theme theme) {
        return isOff(theme) ? "保存成功，关联主题当前已下架" : "成功";
    }

    private static String requireName(String name) {
        String trimmed = trimToNull(name);
        if (trimmed == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "套餐名称不能为空");
        }
        return trimmed;
    }

    private static BigDecimal requirePrice(BigDecimal price) {
        if (price == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "价格不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "价格必须大于 0");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private static int requireRetouchCount(Integer retouchCount) {
        if (retouchCount == null || retouchCount < 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "精修张数必须为不小于 1 的整数");
        }
        return retouchCount;
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PackageVO toVO(Package pkg) {
        return new PackageVO(pkg.getId(), pkg.getThemeId(), pkg.getName(), pkg.getPrice(),
                pkg.getRetouchCount(), pkg.getDuration(), pkg.getIncludes(), pkg.getCoverUrl(),
                pkg.getSort(), pkg.getStatus(), pkg.getCreateTime(), pkg.getUpdateTime());
    }

    /**
     * 保存结果：数据 + 可读提示（关联主题下架时为提示语）。
     */
    public record SaveResult(PackageVO packageVO, String message) {
    }
}
