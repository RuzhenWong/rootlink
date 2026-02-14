-- =====================================================
-- RootLink v0.7.0 数据库迁移脚本
-- 执行前请备份数据库
-- =====================================================

USE rootlink;

-- 1. user_profile 新增排行字段
ALTER TABLE user_profile
  ADD COLUMN family_rank TINYINT NULL COMMENT '家族排行（1=老大,2=老二...）' AFTER marital_status,
  ADD COLUMN province VARCHAR(50) NULL COMMENT '省份' AFTER native_place,
  ADD COLUMN city VARCHAR(50) NULL COMMENT '城市' AFTER province,
  ADD COLUMN district VARCHAR(50) NULL COMMENT '区/县' AFTER city,
  ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT '头像URL' AFTER bio;

-- 2. user_relation 新增关系链、推断来源字段
ALTER TABLE user_relation
  ADD COLUMN relation_chain VARCHAR(500) NULL COMMENT '关系链JSON，如["父","母"]' AFTER relation_desc,
  ADD COLUMN inferred_from BIGINT NULL COMMENT '由哪条关系推断而来（user_relation.id）' AFTER relation_chain,
  ADD COLUMN infer_status TINYINT DEFAULT 0 COMMENT '0-人工建立 1-系统推断待确认 2-推断已确认' AFTER inferred_from;

-- 3. user 表 - allow_search 改为支持身份证搜索
-- （allow_search 原来是允许手机号搜索，现在改为允许身份证搜索，字段复用）

-- 4. testament 新增隐私级别字段
ALTER TABLE testament
  ADD COLUMN visibility TINYINT DEFAULT 0 COMMENT '0-仅自己 1-指定人 2-直系两代 3-全公开' AFTER unlock_status,
  ADD COLUMN visibility_note VARCHAR(200) NULL COMMENT '隐私说明' AFTER visibility;

-- 5. testament_receiver 新增关系类型（用于限制直系可见）
ALTER TABLE testament_receiver
  ADD COLUMN receiver_type TINYINT DEFAULT 0 COMMENT '0-指定用户 1-直系亲属' AFTER receiver_user_id;
