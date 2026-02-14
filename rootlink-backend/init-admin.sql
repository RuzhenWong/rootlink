-- ======================================
-- RootLink 管理员账号初始化
-- 初始密码: Admin@123456
-- 执行前请确认 rootlink 数据库已存在
-- ======================================

USE rootlink;

INSERT INTO `user` (
  `uuid`,
  `phone`,
  `password`,
  `salt`,
  `status`,
  `life_status`,
  `real_name`,
  `real_name_status`,
  `allow_search`,
  `create_time`,
  `update_time`,
  `deleted`
) VALUES (
  '6984c9c4-8da8-4233-9f86-a169c55e0db8',
  '13800000000',
  '130a9778cb484ea85f6ac55fd9ef310324672cf8abbae36fc35cbb04a36e4ce6',
  'rootlink_admin_2026',
  1,      -- 状态正常
  0,      -- 活跃
  '系统管理员',
  2,      -- 已实名
  1,
  NOW(),
  NOW(),
  0
);

-- 为管理员创建 user_profile 记录
INSERT INTO `user_profile` (`user_id`, `create_time`, `update_time`)
SELECT id, NOW(), NOW() FROM `user` WHERE phone = '13800000000' AND deleted = 0;

-- 查询验证
SELECT id, phone, real_name, real_name_status, status FROM `user` WHERE phone = '13800000000';
