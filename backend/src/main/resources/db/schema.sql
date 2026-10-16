-- =====================================================================
-- 解忧摄影系统 数据库初始化脚本（结构）
-- 依据：docs/解忧摄影_数据库设计说明书.docx 2.3 节
-- 环境：MySQL 8.x / InnoDB / utf8mb4
-- 说明：
--   1. 全部使用 IF NOT EXISTS，可重复执行，不会删除已有数据；
--   2. 不使用物理外键，表间通过逻辑外键字段 + 应用层校验关联；
--   3. 执行示例：mysql -u root -p --default-character-set=utf8mb4 < schema.sql
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `photography`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `photography`;

-- ---------------------------------------------------------------------
-- t_user 用户表：用户登录凭证、个人信息与角色
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '登录名',
    `password`    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密后的密码密文',
    `nickname`    VARCHAR(50)  NULL DEFAULT NULL       COMMENT '昵称',
    `phone`       VARCHAR(20)  NULL DEFAULT NULL       COMMENT '手机号',
    `avatar`      VARCHAR(255) NULL DEFAULT NULL       COMMENT '头像 URL',
    `role`        VARCHAR(10)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 正常 / 0 禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ---------------------------------------------------------------------
-- t_theme 拍摄主题表：前台展示的拍摄风格主题
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_theme` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '主题名称（如森系写真）',
    `cover_url`   VARCHAR(255) NULL DEFAULT NULL       COMMENT '封面图 URL',
    `description` VARCHAR(500) NULL DEFAULT NULL       COMMENT '主题描述',
    `tags`        VARCHAR(200) NULL DEFAULT NULL       COMMENT '标签，逗号分隔',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值（越大越靠前）',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 上架 / 0 下架（软下架）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拍摄主题表';

-- ---------------------------------------------------------------------
-- t_package 摄影套餐表：各主题下的报价与服务内容
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_package` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `theme_id`      BIGINT        NOT NULL                COMMENT '所属主题 ID（逻辑外键 → t_theme.id）',
    `name`          VARCHAR(50)   NOT NULL                COMMENT '套餐名称',
    `price`         DECIMAL(10,2) NOT NULL                COMMENT '价格（元）',
    `retouch_count` INT           NOT NULL                COMMENT '精修张数',
    `duration`      VARCHAR(50)   NULL DEFAULT NULL       COMMENT '拍摄时长（如 2 小时）',
    `includes`      VARCHAR(500)  NULL DEFAULT NULL       COMMENT '包含内容说明',
    `cover_url`     VARCHAR(255)  NULL DEFAULT NULL       COMMENT '套餐封面图 URL',
    `sort`          INT           NOT NULL DEFAULT 0      COMMENT '排序值（越大越靠前）',
    `status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1 上架 / 0 下架（软下架）',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_theme_id` (`theme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='摄影套餐表';

-- ---------------------------------------------------------------------
-- t_order 预约订单表：核心业务表与状态流转
-- 状态枚举：PENDING / CONFIRMED / REJECTED / CANCELLED /
--           SHOOTING_DONE / UPLOADED / MODIFYING / DELIVERED
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_order` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`      VARCHAR(32)   NOT NULL                COMMENT '订单编号，业务唯一',
    `user_id`       BIGINT        NOT NULL                COMMENT '下单用户 ID（逻辑外键 → t_user.id）',
    `theme_id`      BIGINT        NOT NULL                COMMENT '拍摄主题 ID（逻辑外键 → t_theme.id）',
    `package_id`    BIGINT        NOT NULL                COMMENT '摄影套餐 ID（逻辑外键 → t_package.id）',
    `shoot_date`    DATE          NOT NULL                COMMENT '拍摄日期',
    `shoot_slot`    VARCHAR(20)   NOT NULL                COMMENT '时间段：MORNING / AFTERNOON / FULL_DAY',
    `remark`        VARCHAR(500)  NULL DEFAULT NULL       COMMENT '用户备注（接口层限制 ≤200 字）',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
    `reject_reason` VARCHAR(500)  NULL DEFAULT NULL       COMMENT '拒绝原因，仅 REJECTED 填写',
    `price`         DECIMAL(10,2) NOT NULL                COMMENT '下单时套餐价格快照',
    `deliver_time`  DATETIME      NULL DEFAULT NULL       COMMENT '确认交付时间',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约订单表';

-- ---------------------------------------------------------------------
-- t_photo 照片表：订单各轮次上传的原图与水印图
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_photo` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`       BIGINT       NOT NULL                COMMENT '所属订单 ID（逻辑外键 → t_order.id）',
    `original_name`  VARCHAR(255) NOT NULL                COMMENT '上传时原始文件名',
    `file_path`      VARCHAR(500) NOT NULL                COMMENT '原图相对路径（uploads/{订单ID}/）',
    `watermark_path` VARCHAR(500) NULL DEFAULT NULL       COMMENT '水印图相对路径（后缀 _wm）',
    `file_size`      BIGINT       NOT NULL DEFAULT 0      COMMENT '文件大小（字节）',
    `upload_round`   INT          NOT NULL DEFAULT 1      COMMENT '上传轮次',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_round` (`order_id`, `upload_round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='照片表';

-- ---------------------------------------------------------------------
-- t_feedback 用户反馈表：用户针对照片轮次的修改意见
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_feedback` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT       NOT NULL                COMMENT '所属订单 ID（逻辑外键 → t_order.id）',
    `round`       INT          NOT NULL DEFAULT 1      COMMENT '反馈轮次（= 已有反馈数 + 1）',
    `photo_round` INT          NOT NULL DEFAULT 1      COMMENT '针对第几轮照片的反馈（对应 t_photo.upload_round）',
    `content`     VARCHAR(500) NOT NULL                COMMENT '修改意见内容（必填，≤500 字）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户反馈表';

-- ---------------------------------------------------------------------
-- t_order_log 订单操作日志表：状态变更时间线
-- 动作枚举：CREATE / CONFIRM / REJECT / CANCEL / SHOOT_DONE / UPLOAD / MODIFY / DELIVER
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_order_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      BIGINT       NOT NULL                COMMENT '订单 ID（逻辑外键 → t_order.id）',
    `operator_id`   BIGINT       NULL DEFAULT NULL       COMMENT '操作人 ID',
    `operator_role` VARCHAR(10)  NULL DEFAULT NULL       COMMENT '操作角色：USER / ADMIN',
    `action`        VARCHAR(50)  NOT NULL                COMMENT '动作，见表注释说明',
    `from_status`   VARCHAR(20)  NULL DEFAULT NULL       COMMENT '变更前状态',
    `to_status`     VARCHAR(20)  NULL DEFAULT NULL       COMMENT '变更后状态',
    `remark`        VARCHAR(500) NULL DEFAULT NULL       COMMENT '备注',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单操作日志表';
