package com.jieyou.photography.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jieyou.photography.common.api.PageQuery;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.catalog.entity.Package;
import com.jieyou.photography.module.catalog.entity.Theme;
import com.jieyou.photography.module.catalog.mapper.PackageMapper;
import com.jieyou.photography.module.catalog.mapper.ThemeMapper;
import com.jieyou.photography.module.feedback.entity.Feedback;
import com.jieyou.photography.module.feedback.mapper.FeedbackMapper;
import com.jieyou.photography.module.feedback.vo.FeedbackVO;
import com.jieyou.photography.module.order.dto.CreateOrderRequest;
import com.jieyou.photography.module.order.dto.RejectOrderRequest;
import com.jieyou.photography.module.order.entity.Order;
import com.jieyou.photography.module.order.entity.OrderLog;
import com.jieyou.photography.module.order.mapper.OrderLogMapper;
import com.jieyou.photography.module.order.mapper.OrderMapper;
import com.jieyou.photography.module.order.mapper.OrderRow;
import com.jieyou.photography.module.order.state.OrderAction;
import com.jieyou.photography.module.order.state.OrderStateMachine;
import com.jieyou.photography.module.order.state.OrderStatus;
import com.jieyou.photography.module.order.state.ShootSlot;
import com.jieyou.photography.module.order.vo.CreateOrderResponse;
import com.jieyou.photography.module.order.vo.OrderDetailVO;
import com.jieyou.photography.module.order.vo.OrderLogVO;
import com.jieyou.photography.module.order.vo.OrderVO;
import com.jieyou.photography.security.LoginUser;
import com.jieyou.photography.security.SecurityUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * 订单业务逻辑，依据 API 约定 5.4 与第 4/6 章：
 * - 状态变更统一使用条件更新（乐观锁），0 行命中返回 40900；
 * - 每次状态变更在同一事务中写入 t_order_log；
 * - 用户只能操作本人订单，管理员不可下单。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int ORDER_NO_MAX_ATTEMPTS = 5;

    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;
    private final ThemeMapper themeMapper;
    private final PackageMapper packageMapper;
    private final FeedbackMapper feedbackMapper;

    public OrderService(OrderMapper orderMapper,
                        OrderLogMapper orderLogMapper,
                        ThemeMapper themeMapper,
                        PackageMapper packageMapper,
                        FeedbackMapper feedbackMapper) {
        this.orderMapper = orderMapper;
        this.orderLogMapper = orderLogMapper;
        this.themeMapper = themeMapper;
        this.packageMapper = packageMapper;
        this.feedbackMapper = feedbackMapper;
    }

    /**
     * 提交预约（POST /api/orders）：仅 USER；套餐上架且属于所选主题；
     * 价格取下单时快照；同事务写入 CREATE 日志。
     */
    @Transactional
    public CreateOrderResponse create(CreateOrderRequest request) {
        LoginUser user = requireUser();
        if (request.shootDate().isBefore(LocalDate.now())) {
            throw new BizException(ResultCode.PARAM_INVALID, "拍摄日期不能早于今天");
        }
        Theme theme = themeMapper.selectById(request.themeId());
        if (theme == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "主题不存在");
        }
        if (theme.getStatus() == null || theme.getStatus() != 1) {
            throw new BizException(ResultCode.STATE_CONFLICT, "主题已下架");
        }
        Package pkg = packageMapper.selectById(request.packageId());
        if (pkg == null || pkg.getStatus() == null || pkg.getStatus() != 1) {
            throw new BizException(ResultCode.STATE_CONFLICT, "套餐已下架或不存在");
        }
        if (!request.themeId().equals(pkg.getThemeId())) {
            throw new BizException(ResultCode.PARAM_INVALID, "套餐不属于所选主题");
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setUserId(user.id());
        order.setThemeId(request.themeId());
        order.setPackageId(request.packageId());
        order.setShootDate(request.shootDate());
        order.setShootSlot(request.shootSlot().name());
        order.setRemark(trimToNull(request.remark()));
        order.setStatus(OrderStatus.PENDING.name());
        order.setPrice(pkg.getPrice());
        order.setCreateTime(now);
        insertOrderWithRetry(order);

        insertLog(order.getId(), user, OrderAction.CREATE, null, OrderStatus.PENDING, null, now);
        return new CreateOrderResponse(order.getId(), order.getOrderNo(), order.getStatus());
    }

    /**
     * 我的订单（GET /api/orders）：仅本人，支持单状态筛选。
     */
    public PageResult<OrderVO> myOrders(OrderStatus status, Integer page, Integer pageSize) {
        LoginUser user = requireUser();
        return selectPage(user.id(), status, null, page, pageSize);
    }

    /**
     * 订单详情（GET /api/orders/{id}）：仅本人，含时间线与历轮反馈。
     */
    public OrderDetailVO userDetail(Long id) {
        LoginUser user = requireUser();
        OrderRow row = requireOrderRow(id);
        requireOwnership(row, user);
        return buildDetail(row);
    }

    /**
     * 取消订单（POST /api/orders/{id}/cancel）：仅本人，仅 PENDING。
     */
    @Transactional
    public OrderVO cancel(Long id) {
        LoginUser user = requireUser();
        OrderRow row = requireOrderRow(id);
        requireOwnership(row, user);
        return transition(id, OrderStatus.PENDING, OrderStatus.CANCELLED, OrderAction.CANCEL,
                user, null, null);
    }

    /**
     * 管理员查看全部订单（GET /api/admin/orders）：状态筛选 + 用户名/主题名关键字模糊搜索。
     */
    public PageResult<OrderVO> adminOrders(OrderStatus status, String keyword, Integer page, Integer pageSize) {
        return selectPage(null, status, trimToNull(keyword), page, pageSize);
    }

    /**
     * 管理员查看订单详情（GET /api/admin/orders/{id}）。
     */
    public OrderDetailVO adminDetail(Long id) {
        return buildDetail(requireOrderRow(id));
    }

    /**
     * 确认订单（POST /api/admin/orders/{id}/confirm）：PENDING → CONFIRMED。
     */
    @Transactional
    public OrderVO adminConfirm(Long id) {
        LoginUser operator = SecurityUtils.currentUser();
        requireOrderRow(id);
        return transition(id, OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderAction.CONFIRM,
                operator, null, null);
    }

    /**
     * 拒绝订单（POST /api/admin/orders/{id}/reject）：PENDING → REJECTED，原因必填。
     */
    @Transactional
    public OrderVO adminReject(Long id, RejectOrderRequest request) {
        LoginUser operator = SecurityUtils.currentUser();
        String reason = trimToNull(request.rejectReason());
        if (reason == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "拒绝原因不能为空");
        }
        requireOrderRow(id);
        return transition(id, OrderStatus.PENDING, OrderStatus.REJECTED, OrderAction.REJECT,
                operator, reason, update -> update.set(Order::getRejectReason, reason));
    }

    /**
     * 标记拍摄完成（POST /api/admin/orders/{id}/shoot-done）：CONFIRMED → SHOOTING_DONE。
     */
    @Transactional
    public OrderVO adminShootDone(Long id) {
        LoginUser operator = SecurityUtils.currentUser();
        requireOrderRow(id);
        return transition(id, OrderStatus.CONFIRMED, OrderStatus.SHOOTING_DONE, OrderAction.SHOOT_DONE,
                operator, null, null);
    }

    /**
     * 照片上传完成后流转为 UPLOADED（SHOOTING_DONE/MODIFYING → UPLOADED），
     * 供照片模块在写入 t_photo 的同一事务中调用。
     */
    @Transactional
    public OrderVO markUploaded(Long id, LoginUser operator) {
        OrderRow row = requireOrderRow(id);
        OrderStatus from = parseStatus(row.getStatus());
        if (from != OrderStatus.SHOOTING_DONE && from != OrderStatus.MODIFYING) {
            throw new BizException(ResultCode.STATE_CONFLICT, "当前订单状态不允许上传照片");
        }
        return transition(id, from, OrderStatus.UPLOADED, OrderAction.UPLOAD, operator, null, null);
    }

    /**
     * 用户提交修改意见后流转为 MODIFYING（UPLOADED → MODIFYING），
     * 供反馈模块在写入 t_feedback 的同一事务中调用。
     */
    @Transactional
    public OrderVO markModifying(Long id, LoginUser operator) {
        requireOrderRow(id);
        return transition(id, OrderStatus.UPLOADED, OrderStatus.MODIFYING, OrderAction.MODIFY,
                operator, null, null);
    }

    /**
     * 确认交付（POST /api/orders/{id}/deliver）：仅本人、仅 UPLOADED；
     * DELIVERED 为终态，写入 deliver_time。
     */
    @Transactional
    public OrderVO deliver(Long id) {
        LoginUser user = requireUser();
        OrderRow row = requireOrderRow(id);
        requireOwnership(row, user);
        LocalDateTime now = LocalDateTime.now();
        return transition(id, OrderStatus.UPLOADED, OrderStatus.DELIVERED, OrderAction.DELIVER,
                user, null, update -> update.set(Order::getDeliverTime, now));
    }

    private static OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ResultCode.STATE_CONFLICT, "订单状态异常");
        }
    }

    private PageResult<OrderVO> selectPage(Long userId, OrderStatus status, String keyword,
                                           Integer page, Integer pageSize) {
        PageQuery query = PageQuery.of(page, pageSize);
        Page<OrderRow> result = orderMapper.selectOrderPage(
                new Page<>(query.page(), query.pageSize()),
                userId,
                status == null ? null : status.name(),
                keyword);
        List<OrderVO> list = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), list);
    }

    /**
     * 统一状态流转：条件更新（仅在当前状态匹配时成功）+ 同事务写日志。
     */
    private OrderVO transition(Long orderId, OrderStatus from, OrderStatus to, OrderAction action,
                               LoginUser operator, String remark,
                               Consumer<LambdaUpdateWrapper<Order>> extraFields) {
        if (OrderStateMachine.actionFor(from, to) != action) {
            throw new IllegalStateException("非法的状态流转定义: " + from + " -> " + to);
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, from.name())
                .set(Order::getStatus, to.name())
                .set(Order::getUpdateTime, now);
        if (extraFields != null) {
            extraFields.accept(update);
        }
        int rows = orderMapper.update(null, update);
        if (rows == 0) {
            throw new BizException(ResultCode.STATE_CONFLICT, conflictMessage(action));
        }
        insertLog(orderId, operator, action, from, to, remark, now);
        return toVO(requireOrderRow(orderId));
    }

    private void insertLog(Long orderId, LoginUser operator, OrderAction action,
                           OrderStatus from, OrderStatus to, String remark, LocalDateTime time) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(operator == null ? null : operator.id());
        log.setOperatorRole(operator == null ? null : operator.role());
        log.setAction(action.name());
        log.setFromStatus(from == null ? null : from.name());
        log.setToStatus(to == null ? null : to.name());
        log.setRemark(remark);
        log.setCreateTime(time);
        orderLogMapper.insert(log);
    }

    /**
     * 订单号：SO + yyyyMMddHHmmss + 6 位随机数（共 22 位，≤32）；
     * 命中 uk_order_no 时重新生成，避免并发重复。
     */
    private void insertOrderWithRetry(Order order) {
        for (int attempt = 0; attempt < ORDER_NO_MAX_ATTEMPTS; attempt++) {
            order.setId(null);
            order.setOrderNo("SO" + LocalDateTime.now().format(ORDER_NO_TIME)
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000)));
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                // 订单号冲突：重试生成
            }
        }
        throw new BizException(ResultCode.DUPLICATE, "订单号生成失败，请重试");
    }

    private OrderDetailVO buildDetail(OrderRow row) {
        List<OrderLogVO> timeline = orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                        .eq(OrderLog::getOrderId, row.getId())
                        .orderByAsc(OrderLog::getCreateTime)
                        .orderByAsc(OrderLog::getId))
                .stream()
                .map(log -> new OrderLogVO(log.getAction(), log.getFromStatus(), log.getToStatus(),
                        log.getRemark(), log.getCreateTime()))
                .toList();
        List<FeedbackVO> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getOrderId, row.getId())
                        .orderByAsc(Feedback::getRound)
                        .orderByAsc(Feedback::getId))
                .stream()
                .map(feedback -> new FeedbackVO(feedback.getId(), feedback.getRound(), feedback.getPhotoRound(),
                        feedback.getContent(), feedback.getCreateTime()))
                .toList();
        return new OrderDetailVO(toVO(row), timeline, feedbacks);
    }

    private OrderRow requireOrderRow(Long id) {
        OrderRow row = orderMapper.selectOrderRow(id);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return row;
    }

    private static void requireOwnership(OrderRow row, LoginUser user) {
        if (!row.getUserId().equals(user.id())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该订单");
        }
    }

    /**
     * 用户端订单操作仅允许 USER；管理员通过 /api/admin/orders 处理。
     */
    private static LoginUser requireUser() {
        LoginUser user = SecurityUtils.currentUser();
        if (!"USER".equals(user.role())) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员不可执行该操作");
        }
        return user;
    }

    private OrderVO toVO(OrderRow row) {
        return new OrderVO(row.getId(), row.getOrderNo(), row.getUserId(), row.getUsername(),
                row.getThemeId(), row.getThemeName(), row.getPackageId(), row.getPackageName(),
                row.getShootDate(), row.getShootSlot(), ShootSlot.textOf(row.getShootSlot()),
                row.getRemark(), row.getStatus(), row.getRejectReason(), row.getPrice(),
                row.getDeliverTime(), row.getCreateTime(), row.getUpdateTime());
    }

    private static String conflictMessage(OrderAction action) {
        return switch (action) {
            case CONFIRM -> "当前订单状态不允许确认";
            case REJECT -> "当前订单状态不允许拒绝";
            case CANCEL -> "当前订单状态不允许取消";
            case SHOOT_DONE -> "当前订单状态不允许标记拍摄完成";
            case DELIVER -> "当前订单状态不允许交付";
            default -> "订单状态已变更，请刷新后重试";
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
