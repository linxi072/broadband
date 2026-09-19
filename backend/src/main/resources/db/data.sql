-- ============================================================================
-- 宽带业务管理系统 · 初始化种子数据（data.sql）
-- 全部使用 INSERT IGNORE + 显式主键 => 幂等，重复启动不会重复插入，
-- 也不会覆盖运行期新增/修改的数据。手动执行：mysql -uroot -p broadband < data.sql
--
-- 【已清理演示数据】本文件原先还包含大量演示业务记录（小区、师傅及其容量、
-- 待派工单、需求登记、SLA 评估记录与赔付工单、套餐及图片/组成项/参数、
-- 客户与合约、流量用量、业务订单、投诉评价、退款发票，以及配套的 dept_id
-- 推导 UPDATE）。这些均为演示数据，已移除；系统开箱即为空业务库，
-- 运营数据请通过后台界面或正式数据导入流程录入。
--
-- 【保留内容（系统运行必需，非演示数据）】
--   1) SLA 规则：SLA 引擎计算承诺时限与赔付标准所依赖的业务规则配置；
--   2) 部门（sys_department）：部门树与行级数据隔离的基础，且被用户 dept_id 引用；
--   3) RBAC 种子：角色 / 菜单权限 / 用户 / 授权 —— 缺失则无任何账号可登录。
--   4) 营销自动化规则（mkt_campaign）：按客户分群触发挽留/续约/关怀的规则配置。
--      用户密码留空，由后端 RbacInitializer 首次启动时写入 BCrypt 哈希。
-- ============================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- SLA 规则（业务规则配置：承诺时限 / 赔付标准，SLA 引擎计算依赖）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sla_rule (id, order_type, sla_name, eval_type, promised_hours, grace_minutes, min_speed_mbps, comp_type, comp_amount, comp_unit, max_comp_amount, enabled) VALUES
 ('R_NEW_TIME',     'NEW_INSTALL', '新装-当日装',        'TIME',  24, 30, 0,   'VOUCHER', 5,  'PER_OVERTIME_HOUR', 20,   1),
 ('R_REPAIR_TIME',  'REPAIR',      '报修-当日修',        'TIME',  24, 30, 0,   'CASH',    20, 'PER_ORDER',         50,   1),
 ('R_MOVE_TIME',    'MOVE',        '移机-当日移',        'TIME',  24, 30, 0,   'CASH',    20, 'PER_ORDER',         50,   1),
 ('R_INSTALL_SPEED','NEW_INSTALL', '装机-网速达标≥500M', 'SPEED',  0,  0, 500, 'CASH',    20, 'PER_ORDER',         NULL, 1);


-- ---------------------------------------------------------------------------
-- 部门（区域 > 分公司树形，行级数据隔离基础，且被 sys_user.dept_id 引用）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_department (id, parent_id, name, region, sort_order, status, created_time) VALUES
 ('D1', NULL, '华南大区',   '华南', 1, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D2', 'D1', '深圳分公司', '华南', 2, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D3', 'D1', '广州分公司', '华南', 3, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D4', NULL, '华东大区',   '华东', 4, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D5', 'D4', '上海分公司', '华东', 5, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000);

-- ===========================================================================
-- RBAC 种子：角色 / 菜单权限 / 用户 / 授权（缺失则无任何账号可登录）
-- 用户密码留空，由 RbacInitializer 首次启动时写入 BCrypt 哈希
-- ===========================================================================
-- 角色
INSERT IGNORE INTO sys_role (id, code, name, remark) VALUES
 ('R_ADMIN',   'ADMIN',    '超级管理员', '全部模块与全部操作权限'),
 ('R_OPERATOR','OPERATOR', '运营专员',   '业务模块全部权限（不含财务/权限管理/性能监控）'),
 ('R_FINANCE', 'FINANCE',  '财务',       '数据看板 + 订单 + 客户 + 财务管理'),
 ('R_CS',      'CS',       '客服',       '数据看板 + 订单 + 客户 + 投诉与评价'),
 ('R_SALES',   'SALES',    '销售',       '数据看板 + 订单 + 客户 + 销售管理');

-- 菜单与权限（path 与前端 router/routes.js 一致；perm 与 @PreAuthorize 一致）
INSERT IGNORE INTO sys_menu (id, parent_id, name, path, perm, type, sort_order) VALUES
 ('M1',  NULL, '数据看板',      '/dashboard',        'dashboard:view', 'MENU',  1),
 ('M2',  NULL, '订单管理',      '/order',            'order:view',     'MENU',  2),
 ('M3',  NULL, '客户管理',      '/customer',         'customer:view',  'MENU',  3),
 ('M4',  NULL, '套餐管理',      '/package',          NULL,             'DIR',   4),
 ('M41', 'M4', '套餐列表',      '/package',          'package:view',   'MENU',  1),
 ('M42', 'M4', '新增/编辑套餐', '/package/edit',     'package:edit',   'MENU',  2),
 ('M43', 'M4', '营销看板',      '/package/marketing', 'package:view',   'MENU',  3),
 ('M5',  NULL, '套餐升级',      '/package/upgrade',  'upgrade:view',   'MENU',  5),
 ('M6',  NULL, '小区覆盖管理',  '/community',        NULL,             'DIR',   6),
 ('M61', 'M6', '小区列表',      '/community',        'community:view', 'MENU',  1),
 ('M62', 'M6', '新增/编辑覆盖', '/community/edit',   'community:edit', 'MENU',  2),
 ('M7',  NULL, '安装工单',      '/workorder/pool',   NULL,             'DIR',   7),
 ('M71', 'M7', '工单池',        '/workorder/pool',     'workorder:view',  'MENU', 1),
 ('M72', 'M7', '派单调度',      '/workorder/dispatch', 'dispatch:run',    'MENU', 2),
 ('M73', 'M7', '容量配置',      '/workorder/capacity', 'capacity:config', 'MENU', 3),
 ('M74', 'M7', '调度规则',      '/workorder/rules',    'capacity:config', 'MENU', 4),
 ('M8',  NULL, '装维 SLA 与赔付','/sla',             'sla:view',       'MENU',  8),
 ('M9',  NULL, '流量监控',      '/traffic',          'traffic:view',   'MENU',  9),
 ('M10', NULL, '投诉与评价',    '/review',           'review:view',    'MENU', 10),
 ('M11', NULL, '销售管理',      '/sales',            'sales:view',     'MENU', 11),
 ('M12', NULL, '财务管理',      '/finance',          'finance:view',   'MENU', 12),
 ('M13', NULL, '权限管理',      '/system/user',      NULL,             'DIR',  13),
 ('M131','M13','用户管理',      '/system/user',      'system:user',    'MENU', 1),
 ('M132','M13','角色管理',      '/system/role',      'system:role',    'MENU', 2),
 ('M133','M13','菜单权限',      '/system/menu',      'system:menu',    'MENU', 3),
 ('M134','M13','操作日志',      '/system/log',       'system:log',     'MENU', 4),
 ('M44','M13','部门管理',      '/system/department','system:dept',   'MENU', 5),
 ('M14', NULL, '性能监控',      '/monitor',          'monitor:view',   'MENU', 14);

-- 角色授权（管理员：全部菜单）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
 ('R_ADMIN','M1'),('R_ADMIN','M2'),('R_ADMIN','M3'),('R_ADMIN','M4'),('R_ADMIN','M41'),('R_ADMIN','M42'),
 ('R_ADMIN','M5'),('R_ADMIN','M6'),('R_ADMIN','M61'),('R_ADMIN','M62'),('R_ADMIN','M7'),('R_ADMIN','M71'),
 ('R_ADMIN','M72'),('R_ADMIN','M73'),('R_ADMIN','M74'),('R_ADMIN','M8'),('R_ADMIN','M9'),('R_ADMIN','M10'),
 ('R_ADMIN','M11'),('R_ADMIN','M12'),('R_ADMIN','M13'),('R_ADMIN','M131'),('R_ADMIN','M132'),('R_ADMIN','M133'),
 ('R_ADMIN','M134'),('R_ADMIN','M14'),('R_ADMIN','M43'),('R_ADMIN','M44'),
 -- 运营专员：业务模块（不含财务 / 权限管理 / 性能监控）
 ('R_OPERATOR','M1'),('R_OPERATOR','M2'),('R_OPERATOR','M3'),('R_OPERATOR','M4'),('R_OPERATOR','M41'),('R_OPERATOR','M43'),
 ('R_OPERATOR','M42'),('R_OPERATOR','M5'),('R_OPERATOR','M6'),('R_OPERATOR','M61'),('R_OPERATOR','M62'),
 ('R_OPERATOR','M7'),('R_OPERATOR','M71'),('R_OPERATOR','M72'),('R_OPERATOR','M73'),('R_OPERATOR','M74'),
 ('R_OPERATOR','M8'),('R_OPERATOR','M9'),('R_OPERATOR','M10'),('R_OPERATOR','M11'),
 -- 财务
 ('R_FINANCE','M1'),('R_FINANCE','M2'),('R_FINANCE','M3'),('R_FINANCE','M12'),
 -- 客服
 ('R_CS','M1'),('R_CS','M2'),('R_CS','M3'),('R_CS','M10'),
 -- 销售
 ('R_SALES','M1'),('R_SALES','M2'),('R_SALES','M3'),('R_SALES','M11');

-- 用户（password 留空 = 待 RbacInitializer 写入初始密码；dept_id 关联 sys_department）
-- admin 无部门 -> 看全部；其余按所属分公司隔离（深圳 D2 / 上海 D5）
INSERT IGNORE INTO sys_user (id, username, password, name, dept_id, status, created_time) VALUES
 ('U_ADMIN',   'admin',    '', '超级管理员', NULL,    'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('U_LIUWEI',  'liuwei',   '', '刘伟',       'D2',    'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('U_ZHAOMIN', 'zhaomin',  '', '赵敏',       'D2',    'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('U_WANGFANG','wangfang', '', '王芳',       'D2',    'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('U_SH',      'shanghai', '', '上海运营',   'D5',    'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000);

-- 幂等补全（已存在用户不会被 INSERT 覆盖，用 UPDATE 同步部门归属）
UPDATE sys_user SET dept_id = NULL WHERE username = 'admin';
-- T-02 安全治理：默认管理员首次登录强制改密（幂等，重复执行无副作用）
UPDATE sys_user SET must_change_password = 1 WHERE username = 'admin';
UPDATE sys_user SET dept_id = 'D2'  WHERE username = 'liuwei';
UPDATE sys_user SET dept_id = 'D2'  WHERE username = 'zhaomin';
UPDATE sys_user SET dept_id = 'D2'  WHERE username = 'wangfang';
UPDATE sys_user SET dept_id = 'D5'  WHERE username = 'shanghai';

-- 用户角色
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
 ('U_ADMIN','R_ADMIN'),
 ('U_LIUWEI','R_OPERATOR'),
 ('U_ZHAOMIN','R_FINANCE'),
 ('U_WANGFANG','R_CS'),
 ('U_SH','R_OPERATOR');

-- 营销自动化规则（V1.15 数据智能）：按分群触发挽留/续约/关怀动作
INSERT IGNORE INTO mkt_campaign (id, name, trigger_type, action_type, target_item, status, description, created_time, updated_time) VALUES
 ('MK_CHURN','流失预警挽留','CHURN_RISK','GRANT_COUPON','PM_VOUCHER10','ENABLED','对 180 天无互动或已流失风险客户自动发放 10 元现金券挽留',UNIX_TIMESTAMP()*1000,UNIX_TIMESTAMP()*1000),
 ('MK_RENEW','临期客户续约推送','RENEW','SEND_PROMO','PR_ANNUAL','ENABLED','对 90 天内合约到期客户推送年度套餐续约优惠',UNIX_TIMESTAMP()*1000,UNIX_TIMESTAMP()*1000),
 ('MK_VIP','高价值客户专属提速','HIGH_VALUE','GRANT_COUPON','PM_SPEED500','ENABLED','对近 30 天活跃的高价值(VIP/四星)客户发放提速至 500M 券',UNIX_TIMESTAMP()*1000,UNIX_TIMESTAMP()*1000);
