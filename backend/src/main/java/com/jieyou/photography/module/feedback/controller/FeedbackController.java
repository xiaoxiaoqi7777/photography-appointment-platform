package com.jieyou.photography.module.feedback.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.module.feedback.dto.FeedbackRequest;
import com.jieyou.photography.module.feedback.service.FeedbackService;
import com.jieyou.photography.module.feedback.vo.FeedbackVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 修改反馈接口，依据 API 约定 5.6（USER 仅本人，历史 ADMIN 可查）。
 */
@RestController
@RequestMapping("/api/orders/{orderId}")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/feedback")
    public ApiResponse<FeedbackVO> submit(@PathVariable Long orderId,
                                          @Valid @RequestBody FeedbackRequest request) {
        return ApiResponse.success(feedbackService.submit(orderId, request));
    }

    @GetMapping("/feedbacks")
    public ApiResponse<List<FeedbackVO>> list(@PathVariable Long orderId) {
        return ApiResponse.success(feedbackService.list(orderId));
    }
}
