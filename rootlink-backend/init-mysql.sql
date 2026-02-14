-- =====================================================
-- RootLink MySQL 数据库完整初始化脚本
-- 版本: v0.7.0
-- 日期: 2026-02-12
-- 说明: 全量脚本，全新部署直接执行即可
--       包含: 用户系统 / 亲属关系 / 关系推断 /
--             生前遗言 / 离世状态 / 挽联缅怀
-- 执行: mysql -u rootlink -p rootlink < init-mysql.sql
--   或: mysql -u root -p < init-mysql.sql
-- =====================================================

-- -----------------------------------------------
-- 0. 创建数据库 & 用户 & 授权（root执行）
-- -----------------------------------------------
CREATE DATABASE IF NOT EXISTS rootlink
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 如需创建专用用户请取消注释：
-- CREATE USER IF NOT EXISTS 'rootlink'@'localhost' IDENTIFIED BY '19930212MAOyue';
-- GRANT ALL PRIVILEGES ON rootlink.* TO 'rootlink'@'localhost';
-- FLUSH PRIVILEGES;

USE rootlink;

-- -----------------------------------------------
-- 安全起见：先删旧表（全新部署时打开注释）
-- -----------------------------------------------
-- SET FOREIGN_KEY_CHECKS = 0;
-- DROP TABLE IF EXISTS login_log, eulogy_reviewer, eulogy,
--   death_status_log, death_proof, death_mark,
--   testament_receiver, testament,
--   user_relation_apply, user_relation,
--   user_profile, user;
-- SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 表结构
-- =====================================================

-- -----------------------------------------------
-- 1. 用户基础信息表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `uuid`              VARCHAR(64)  NOT NULL COMMENT '用户UUID',
  `phone`             VARCHAR(20)  NOT NULL COMMENT '手机号',
  `email`             VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `password`          VARCHAR(128) NOT NULL COMMENT '密码（SHA-256+盐）',
  `salt`              VARCHAR(64)  NOT NULL COMMENT '密码盐值',
  `real_name`         VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
  `id_card_encrypted` VARCHAR(256) DEFAULT NULL COMMENT '身份证号（AES-256加密）',
  `id_card_hash`      VARCHAR(64)  DEFAULT NULL COMMENT '身份证号哈希（唯一性校验）',
  `real_name_status`  TINYINT      NOT NULL DEFAULT 0
      COMMENT '实名状态: 0-未实名 1-审核中 2-已实名 3-审核失败',
  `real_name_time`    DATETIME     DEFAULT NULL COMMENT '实名通过时间',
  `status`            TINYINT      NOT NULL DEFAULT 1
      COMMENT '账号状态: 0-禁用 1-正常 2-冻结 3-已注销',
  `life_status`       TINYINT      NOT NULL DEFAULT 0
      COMMENT '生命状态: 0-活跃 1-不活跃 2-疑似离世 3-已离世',
  `last_login_time`   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip`     VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
  `allow_search`      TINYINT      NOT NULL DEFAULT 1
      COMMENT '允许身份证号搜索: 0-否 1-是',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uuid`         (`uuid`),
  UNIQUE KEY `uk_phone`        (`phone`),
  UNIQUE KEY `uk_id_card_hash` (`id_card_hash`),
  KEY `idx_real_name_status` (`real_name_status`),
  KEY `idx_life_status`      (`life_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础信息表';

-- -----------------------------------------------
-- 2. 用户详细信息表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `user_profile` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
  `gender`         TINYINT      DEFAULT 0 COMMENT '0-未知 1-男 2-女',
  `nation`         VARCHAR(50)  DEFAULT NULL COMMENT '民族',
  `birth_date`     DATE         DEFAULT NULL COMMENT '出生日期',
  `death_date`     DATE         DEFAULT NULL COMMENT '离世日期',
  `native_place`   VARCHAR(100) DEFAULT NULL COMMENT '籍贯（兼容旧数据）',
  `province`       VARCHAR(50)  DEFAULT NULL COMMENT '籍贯-省份',
  `city`           VARCHAR(50)  DEFAULT NULL COMMENT '籍贯-城市',
  `district`       VARCHAR(50)  DEFAULT NULL COMMENT '籍贯-区县',
  `residence`      VARCHAR(200) DEFAULT NULL COMMENT '居住地',
  `wechat`         VARCHAR(100) DEFAULT NULL COMMENT '微信号',
  `qq`             VARCHAR(20)  DEFAULT NULL COMMENT 'QQ号',
  `work_unit`      VARCHAR(200) DEFAULT NULL COMMENT '工作单位',
  `position`       VARCHAR(100) DEFAULT NULL COMMENT '职务',
  `education`      VARCHAR(50)  DEFAULT NULL COMMENT '学历',
  `marital_status` TINYINT      DEFAULT 0 COMMENT '婚姻: 0-未知 1-未婚 2-已婚 3-离异 4-丧偶',
  `family_rank`    TINYINT      DEFAULT NULL COMMENT '家族排行（1=老大，用于二叔/三姨称谓）',
  `bio`            TEXT         DEFAULT NULL COMMENT '个人简介',
  `avatar_url`     VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户详细信息表';

-- -----------------------------------------------
-- 3. 用户关系表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `user_relation` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT       NOT NULL COMMENT '关系持有方',
  `related_user_id` BIGINT       NOT NULL COMMENT '关联用户',
  `relation_type`   TINYINT      NOT NULL DEFAULT 99
      COMMENT '1-父亲 2-母亲 3-配偶 4-子女 5-同辈 99-其他',
  `relation_desc`   VARCHAR(50)  DEFAULT NULL COMMENT '中文称谓（奶奶/表哥/姥爷等）',
  `relation_chain`  VARCHAR(500) DEFAULT NULL COMMENT '关系链JSON，如["父","母"]',
  `inferred_from`   BIGINT       DEFAULT NULL COMMENT '由哪条关系推断而来',
  `infer_status`    TINYINT      NOT NULL DEFAULT 0
      COMMENT '0-手动建立 1-系统推断待确认 2-推断已确认',
  `confirm_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待确认 1-已确认 2-已拒绝',
  `confirm_time`    DATETIME     DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id`         (`user_id`),
  KEY `idx_related_user_id` (`related_user_id`),
  KEY `idx_confirm_status`  (`confirm_status`),
  KEY `idx_infer_status`    (`infer_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关系表';

-- -----------------------------------------------
-- 4. 关系申请表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `user_relation_apply` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `applicant_user_id` BIGINT       NOT NULL COMMENT '申请人ID',
  `target_user_id`    BIGINT       NOT NULL COMMENT '目标用户ID',
  `relation_type`     TINYINT      NOT NULL DEFAULT 99,
  `relation_desc`     VARCHAR(50)  DEFAULT NULL COMMENT '中文称谓',
  `reason`            VARCHAR(500) DEFAULT NULL
      COMMENT '格式: [CHAIN]<链JSON> <用户留言>',
  `apply_status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待处理 1-同意 2-拒绝',
  `handle_time`       DATETIME     DEFAULT NULL,
  `reject_reason`     VARCHAR(500) DEFAULT NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_applicant_user_id` (`applicant_user_id`),
  KEY `idx_target_user_id`    (`target_user_id`),
  KEY `idx_apply_status`      (`apply_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关系申请表';

-- -----------------------------------------------
-- 5. 生前遗言表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `testament` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`           BIGINT       NOT NULL COMMENT '创建人',
  `title`             VARCHAR(200) NOT NULL COMMENT '标题',
  `content_encrypted` TEXT         NOT NULL COMMENT '内容（TODO: AES-256加密）',
  `testament_type`    TINYINT      NOT NULL DEFAULT 1
      COMMENT '1-文字遗言 2-给特定人的信 3-财产分配 4-其他',
  `unlock_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0-锁定 1-已解锁',
  `visibility`        TINYINT      NOT NULL DEFAULT 0
      COMMENT '0-仅自己 1-指定人 2-直系两代内 3-全公开',
  `visibility_note`   VARCHAR(200) DEFAULT NULL COMMENT '隐私说明',
  `unlock_time`       DATETIME     DEFAULT NULL,
  `attachment_urls`   TEXT         DEFAULT NULL COMMENT '附件URL列表（JSON）',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id`        (`user_id`),
  KEY `idx_testament_type` (`testament_type`),
  KEY `idx_unlock_status`  (`unlock_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生前遗言表';

-- -----------------------------------------------
-- 6. 遗言接收人表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `testament_receiver` (
  `id`               BIGINT   NOT NULL AUTO_INCREMENT,
  `testament_id`     BIGINT   NOT NULL COMMENT '遗言ID',
  `receiver_user_id` BIGINT   NOT NULL COMMENT '接收人ID',
  `receiver_type`    TINYINT  NOT NULL DEFAULT 0 COMMENT '0-指定用户 1-直系亲属',
  `has_read`         TINYINT  NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
  `read_time`        DATETIME DEFAULT NULL,
  `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_testament_id`     (`testament_id`),
  KEY `idx_receiver_user_id` (`receiver_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='遗言接收人表';

-- -----------------------------------------------
-- 7. 离世标记表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `death_mark` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `target_user_id` BIGINT       NOT NULL COMMENT '被标记用户',
  `marker_user_id` BIGINT       NOT NULL COMMENT '标记人',
  `relation_id`    BIGINT       DEFAULT NULL,
  `mark_reason`    VARCHAR(500) NOT NULL,
  `mark_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target_user_id` (`target_user_id`),
  KEY `idx_marker_user_id` (`marker_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离世标记表';

-- -----------------------------------------------
-- 8. 离世证明材料表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `death_proof` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `target_user_id`   BIGINT       NOT NULL,
  `uploader_user_id` BIGINT       NOT NULL,
  `proof_type`       TINYINT      NOT NULL COMMENT '1-死亡证明 2-火化证明 3-讣告 4-其他',
  `proof_url`        VARCHAR(500) NOT NULL,
  `description`      VARCHAR(500) DEFAULT NULL,
  `audit_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待审核 1-通过 2-拒绝',
  `auditor_id`       BIGINT       DEFAULT NULL,
  `audit_time`       DATETIME     DEFAULT NULL,
  `audit_reason`     VARCHAR(500) DEFAULT NULL,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target_user_id` (`target_user_id`),
  KEY `idx_audit_status`   (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离世证明材料表';

-- -----------------------------------------------
-- 9. 离世状态变更日志
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `death_status_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `old_status`    TINYINT      NOT NULL,
  `new_status`    TINYINT      NOT NULL,
  `change_reason` VARCHAR(500) NOT NULL,
  `operator_id`   BIGINT       DEFAULT NULL,
  `mark_count`    INT          DEFAULT NULL,
  `proof_count`   INT          DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id`     (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离世状态变更日志';

-- -----------------------------------------------
-- 10. 挽联表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `eulogy` (
  `id`                BIGINT   NOT NULL AUTO_INCREMENT,
  `target_user_id`    BIGINT   NOT NULL COMMENT '缅怀对象',
  `submitter_user_id` BIGINT   NOT NULL COMMENT '提交人',
  `content`           TEXT     NOT NULL,
  `submit_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `review_status`     TINYINT  NOT NULL DEFAULT 0 COMMENT '0-待审核 1-通过 2-拒绝',
  `publish_time`      DATETIME DEFAULT NULL,
  `reject_reason`     VARCHAR(500) DEFAULT NULL,
  `rejector_user_id`  BIGINT   DEFAULT NULL,
  `reject_time`       DATETIME DEFAULT NULL,
  `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_target_user_id`    (`target_user_id`),
  KEY `idx_submitter_user_id` (`submitter_user_id`),
  KEY `idx_review_status`     (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挽联表';

-- -----------------------------------------------
-- 11. 挽联审核人表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `eulogy_reviewer` (
  `id`               BIGINT   NOT NULL AUTO_INCREMENT,
  `eulogy_id`        BIGINT   NOT NULL,
  `reviewer_user_id` BIGINT   NOT NULL,
  `relation_type`    TINYINT  NOT NULL COMMENT '1-父母 2-配偶 3-子女 4-祖父母 5-孙子女',
  `review_status`    TINYINT  NOT NULL DEFAULT 0 COMMENT '0-待审核 1-通过 2-拒绝',
  `review_time`      DATETIME DEFAULT NULL,
  `review_opinion`   VARCHAR(500) DEFAULT NULL,
  `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eulogy_id`        (`eulogy_id`),
  KEY `idx_reviewer_user_id` (`reviewer_user_id`),
  UNIQUE KEY `uk_eulogy_reviewer` (`eulogy_id`, `reviewer_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挽联审核人表';

-- -----------------------------------------------
-- 12. 登录日志表
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS `login_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL,
  `login_time`   DATETIME     NOT NULL,
  `login_ip`     VARCHAR(50)  DEFAULT NULL,
  `device_type`  VARCHAR(50)  DEFAULT NULL COMMENT 'web/android/ios/harmonyos',
  `device_info`  VARCHAR(200) DEFAULT NULL,
  `login_status` TINYINT      NOT NULL COMMENT '0-失败 1-成功',
  `fail_reason`  VARCHAR(200) DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';


-- =====================================================
-- 初始化管理员账号
-- 手机号: 13800000000
-- 密码: Admin@123456
-- =====================================================
INSERT INTO `user` (
  `uuid`, `phone`, `email`, `password`, `salt`,
  `real_name`, `real_name_status`, `status`, `life_status`, `allow_search`
) VALUES (
  'admin-00000000-0000-0000-0000-000000000001',
  '13800000000',
  'admin@rootlink.com',
  '130a9778cb484ea85f6ac55fd9ef310324672cf8abbae36fc35cbb04a36e4ce6',
  'rootlink_admin_2026',
  '系统管理员',
  2, 1, 0, 0
) ON DUPLICATE KEY UPDATE `email` = VALUES(`email`);


-- =====================================================
-- 验证
-- =====================================================
SHOW TABLES;
SELECT CONCAT('RootLink v0.7.0 初始化完成，共 ', COUNT(*), ' 张表') AS result
FROM information_schema.tables
WHERE table_schema = DATABASE();
