package com.jieyou.photography.module.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jieyou.photography.common.api.ResultCode;
import com.jieyou.photography.common.exception.BizException;
import com.jieyou.photography.module.feedback.dto.FeedbackRequest;
import com.jieyou.photography.module.feedback.entity.Feedback;
import com.jieyou.photography.module.feedback.mapper.FeedbackMapper;
import com.jieyou.photography.module.feedback.vo.FeedbackVO;
import com.jieyou.photography.module.order.mapper.OrderMapper;
import com.jieyou.photography.module.order.mapper.OrderRow;
import com.jieyou.photography.module.order.service.OrderService;
import com.jieyou.photography.module.photo.entity.Photo;
import com.jieyou.photography.module.photo.mapper.PhotoMapper;
import com.jieyou.photography.security.LoginUser;
import com.jieyou.photography.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 修改反馈模块，依据 API 约定 5.6：
 * - 仅本人、仅 UPLOADED 可提交；content 必填 ≤500；
 * - round = 已有反馈数 + 1，photo_round = 当前照片最大轮次（后端推导）；
 * - 提交后订单 UPLOADED → MODIFYING，同事务写 t_order_log(MODIFY)。
 */
@Service
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final PhotoMapper photoMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public FeedbackService(FeedbackMapper feedbackMapper,
                           PhotoMapper photoMapper,
                           OrderMapper orderMapper,
                           OrderService orderService) {
        this.feedbackMapper = feedbackMapper;
        this.photoMapper = photoMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Transactional
    public FeedbackVO submit(Long orderId, FeedbackRequest request) {
        LoginUser user = requireUser();
        OrderRow order = requireOrder(orderId);
        requireOwnership(order, user);
        if (!"UPLOADED".equals(order.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT, "当前订单状态不允许提交修改意见");
        }

        String content = trimToNull(request.content());
        if (content == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "修改意见不能为空");
        }

        int round = Math.toIntExact(feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getOrderId, orderId))) + 1;
        Feedback feedback = new Feedback();
        feedback.setOrderId(orderId);
        feedback.setRound(round);
        feedback.setPhotoRound(currentPhotoRound(orderId));
        feedback.setContent(content);
        feedback.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);

        orderService.markModifying(orderId, user);
        return toVO(feedback);
    }

    /**
     * 反馈历史（GET /feedbacks）：USER 仅本人，ADMIN 可查看全部，按轮次升序。
     */
    public List<FeedbackVO> list(Long orderId) {
        LoginUser user = SecurityUtils.currentUser();
        OrderRow order = requireOrder(orderId);
        if (!"ADMIN".equals(user.role())) {
            requireOwnership(order, user);
        }
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getOrderId, orderId)
                        .orderByAsc(Feedback::getRound)
                        .orderByAsc(Feedback::getId))
                .stream()
                .map(FeedbackService::toVO)
                .toList();
    }

    /**
     * 当前照片轮次：t_photo.upload_round 最大值；无照片时为 0。
     */
    private int currentPhotoRound(Long orderId) {
        List<Photo> latest = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getOrderId, orderId)
                .orderByDesc(Photo::getUploadRound)
                .last("LIMIT 1"));
        return latest.isEmpty() ? 0 : latest.get(0).getUploadRound();
    }

    private OrderRow requireOrder(Long orderId) {
        OrderRow order = orderMapper.selectOrderRow(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private static LoginUser requireUser() {
        LoginUser user = SecurityUtils.currentUser();
        if (!"USER".equals(user.role())) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员不可执行该操作");
        }
        return user;
    }

    private static void requireOwnership(OrderRow order, LoginUser user) {
        if (!order.getUserId().equals(user.id())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该订单");
        }
    }

    private static FeedbackVO toVO(Feedback feedback) {
        return new FeedbackVO(feedback.getId(), feedback.getRound(), feedback.getPhotoRound(),
                feedback.getContent(), feedback.getCreateTime());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
