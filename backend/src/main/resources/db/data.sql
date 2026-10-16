-- =====================================================================
-- 解忧摄影系统 初始化数据脚本
-- 依据：docs/解忧摄影_需求分析说明书.docx（ADMIN 由初始化脚本预置，不开放注册）
-- 说明：
--   1. 请先执行 schema.sql 再执行本脚本；
--   2. 全部使用存在性判断，可重复执行不重复插入；
--   3. admin 初始密码：admin123456（BCrypt 密文存储，首登后请修改）。
-- 执行示例：mysql -u root -p --default-character-set=utf8mb4 < data.sql
-- =====================================================================

SET NAMES utf8mb4;

USE `photography`;

-- ---------------------------------------------------------------------
-- 1. 预置管理员账号：admin / admin123456（BCrypt）
-- ---------------------------------------------------------------------
INSERT INTO `t_user` (`username`, `password`, `nickname`, `phone`, `role`, `status`)
SELECT 'admin', '$2a$10$AuMvgk4ttu0J9b.1vMNS5uDLF7DaBf0jVEZ9s8slX2Sk/Sak7yxMi',
       '平台管理员', NULL, 'ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM `t_user` WHERE `username` = 'admin');

-- ---------------------------------------------------------------------
-- 2. 演示主题（全部上架）
-- ---------------------------------------------------------------------
INSERT INTO `t_theme` (`name`, `cover_url`, `description`, `tags`, `sort`, `status`)
SELECT '森系写真', NULL,
       '走进自然森林，捕捉清新灵动的个人写真瞬间，适合第一次拍写真的你。',
       '森系,清新,自然', 90, 1
WHERE NOT EXISTS (SELECT 1 FROM `t_theme` WHERE `name` = '森系写真');

INSERT INTO `t_theme` (`name`, `cover_url`, `description`, `tags`, `sort`, `status`)
SELECT '婚纱照', NULL,
       '内外景双机位拍摄，记录属于你们的浪漫仪式感，半日/全日两种方案可选。',
       '婚纱,浪漫,仪式感', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM `t_theme` WHERE `name` = '婚纱照');

INSERT INTO `t_theme` (`name`, `cover_url`, `description`, `tags`, `sort`, `status`)
SELECT '全家福', NULL,
       '三代同框的温馨时刻，专业引导摆拍，留住全家人的幸福笑容。',
       '全家福,温馨,亲子', 80, 1
WHERE NOT EXISTS (SELECT 1 FROM `t_theme` WHERE `name` = '全家福');

-- ---------------------------------------------------------------------
-- 3. 演示套餐（全部上架，分别挂在上述主题下）
-- ---------------------------------------------------------------------
INSERT INTO `t_package`
    (`theme_id`, `name`, `price`, `retouch_count`, `duration`, `includes`, `cover_url`, `sort`, `status`)
SELECT t.`id`, '森系轻写真', 399.00, 10, '2 小时',
       '场地协调 1 次、服装 1 套、精修 10 张、全部原片赠送', NULL, 90, 1
FROM `t_theme` t
WHERE t.`name` = '森系写真'
  AND NOT EXISTS (SELECT 1 FROM `t_package` p WHERE p.`name` = '森系轻写真');

INSERT INTO `t_package`
    (`theme_id`, `name`, `price`, `retouch_count`, `duration`, `includes`, `cover_url`, `sort`, `status`)
SELECT t.`id`, '森系全写真', 699.00, 20, '3 小时',
       '场地协调 1 次、服装 2 套、精修 20 张、全部原片赠送、10 寸相册 1 本', NULL, 85, 1
FROM `t_theme` t
WHERE t.`name` = '森系写真'
  AND NOT EXISTS (SELECT 1 FROM `t_package` p WHERE p.`name` = '森系全写真');

INSERT INTO `t_package`
    (`theme_id`, `name`, `price`, `retouch_count`, `duration`, `includes`, `cover_url`, `sort`, `status`)
SELECT t.`id`, '婚纱半日拍', 1999.00, 30, '4 小时',
       '内外景任选 1 景、服装 3 套、精修 30 张、全部原片赠送', NULL, 100, 1
FROM `t_theme` t
WHERE t.`name` = '婚纱照'
  AND NOT EXISTS (SELECT 1 FROM `t_package` p WHERE p.`name` = '婚纱半日拍');

INSERT INTO `t_package`
    (`theme_id`, `name`, `price`, `retouch_count`, `duration`, `includes`, `cover_url`, `sort`, `status`)
SELECT t.`id`, '婚纱全日拍', 3299.00, 50, '8 小时',
       '内外景任选 2 景、服装 5 套、精修 50 张、全部原片赠送、12 寸相册 2 本、摆台 1 个', NULL, 95, 1
FROM `t_theme` t
WHERE t.`name` = '婚纱照'
  AND NOT EXISTS (SELECT 1 FROM `t_package` p WHERE p.`name` = '婚纱全日拍');

INSERT INTO `t_package`
    (`theme_id`, `name`, `price`, `retouch_count`, `duration`, `includes`, `cover_url`, `sort`, `status`)
SELECT t.`id`, '全家福标准', 599.00, 15, '2 小时',
       '服装提供（至多 5 人）、精修 15 张、全部原片赠送', NULL, 80, 1
FROM `t_theme` t
WHERE t.`name` = '全家福'
  AND NOT EXISTS (SELECT 1 FROM `t_package` p WHERE p.`name` = '全家福标准');
