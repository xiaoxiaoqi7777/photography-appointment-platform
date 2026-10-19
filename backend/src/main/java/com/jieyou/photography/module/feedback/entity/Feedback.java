package com.jieyou.photography.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户反馈表 t_feedback，结构依据《数据库设计》2.3.6 节。
 * 阶段五订单详情需要展示历轮反馈，提交接口在阶段七实现。
 */
@TableName("t_feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Integer round;
    private Integer photoRound;
    private String content;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public Integer getPhotoRound() {
        return photoRound;
    }

    public void setPhotoRound(Integer photoRound) {
        this.photoRound = photoRound;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
