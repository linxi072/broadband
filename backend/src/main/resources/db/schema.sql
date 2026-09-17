-- ============================================================================
-- 宽带业务管理系统 · MySQL 建表脚本（schema.sql）
-- 由 Spring Boot 启动时自动执行（spring.sql.init），全部为幂等写法，
-- 可安全重复执行。手动执行：mysql -uroot -p broadband < schema.sql
--
-- 命名约定：表名/列名为 snake_case；entity 字段为 camelCase（MyBatis-Plus 自动映射）。
-- 两个保留字/歧义列做了「列名 vs 接口字段」桥接（见对应 mapper 的 @Select）：
--   package_converge_item.description  -> 接口字段 desc
--   package_param_option.option_value  -> 接口字段 value
-- ============================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 小区覆盖点（community 模块 / install 模块共用）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS community (
  id           VARCHAR(32)  NOT NULL COMMENT '小区ID',
  name         VARCHAR(128) NOT NULL COMMENT '小区名称',
  region       VARCHAR(64)           COMMENT '所属片区，如 南山',
  street       VARCHAR(64)           COMMENT '所属街道，如 科技园路',
  latitude     DOUBLE                COMMENT '纬度（相邻性判定用）',
  longitude    DOUBLE                COMMENT '经度（相邻性判定用）',
  installable  TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否可安装 1/0',
  port_total   INT          NOT NULL DEFAULT 0 COMMENT '端口总数',
  port_used    INT          NOT NULL DEFAULT 0 COMMENT '已用端口数',
  PRIMARY KEY (id),
  KEY idx_community_name (name),
  KEY idx_community_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小区覆盖点';

-- ---------------------------------------------------------------------------
-- 2. 安装师傅
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS worker (
  id          VARCHAR(32)  NOT NULL COMMENT '师傅ID',
  name        VARCHAR(64)  NOT NULL COMMENT '姓名',
  phone       VARCHAR(32)           COMMENT '登录手机号（师傅端 worker-login 凭此校验）',
  region      VARCHAR(64)           COMMENT '负责片区',
  skill_level INT          NOT NULL DEFAULT 1 COMMENT '技能等级（可用于容量微调）',
  PRIMARY KEY (id),
  KEY idx_worker_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装师傅';

-- ---------------------------------------------------------------------------
-- 3. 师傅时段容量配置（覆盖 CapacityPolicy 默认：相邻 3-4 单、非相邻 1-2 单）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS worker_capacity (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  worker_id        VARCHAR(32)  NOT NULL COMMENT '师傅ID',
  time_slot        VARCHAR(64)  NOT NULL COMMENT '时段，如 2026-09-15#AM',
  adjacent_cap     INT                   COMMENT '相邻小区每时段上限（默认4，下限3）',
  non_adjacent_cap INT                   COMMENT '非相邻小区每时段上限（默认2，下限1）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_worker_slot (worker_id, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='师傅时段容量配置';

-- ---------------------------------------------------------------------------
-- 4. 安装工单
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_order (
  id             VARCHAR(32)  NOT NULL COMMENT '工单ID',
  community_id   VARCHAR(32)           COMMENT '关联 community.id',
  address        VARCHAR(255)          COMMENT '安装地址',
  time_slot      VARCHAR(64)           COMMENT '派单时段',
  customer_name  VARCHAR(64)           COMMENT '客户姓名',
  package_desc   VARCHAR(128)          COMMENT '办理套餐描述',
  status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ASSIGNED/INSTALLING/DONE/CANCELLED',
  worker_id      VARCHAR(32)           COMMENT '指派师傅（派单回填）',
  cluster_id     VARCHAR(32)           COMMENT '相邻聚类簇ID（派单回填）',
  adjacent_route TINYINT(1)            COMMENT '是否走相邻合并路线（派单回填）',
  down_speed     INT                   COMMENT '装机测速下行（Mbps，完工回填）',
  up_speed       INT                   COMMENT '装机测速上行（Mbps，完工回填）',
  sign_name      VARCHAR(64)           COMMENT '客户签名（完工回填）',
  service_items  VARCHAR(255)          COMMENT '六项服务确认结果（完工回填）',
  complete_time  DATETIME              COMMENT '完工时间',
  biz_order_id   VARCHAR(32)           COMMENT '关联业务订单 biz_order.id（业务闭环：下单→支付→派单→装机→赔付→评价）',
  PRIMARY KEY (id),
  KEY idx_wo_status_slot (status, time_slot),
  KEY idx_wo_community (community_id),
  KEY idx_wo_worker (worker_id),
  KEY idx_wo_biz_order (biz_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装工单';

-- ---------------------------------------------------------------------------
-- 5. 安装需求登记（小区未覆盖时客户登记）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS community_demand (
  id         VARCHAR(32)  NOT NULL COMMENT '登记ID',
  name       VARCHAR(128)          COMMENT '登记的小区名',
  contact    VARCHAR(64)           COMMENT '联系人',
  phone      VARCHAR(32)           COMMENT '联系电话',
  note       VARCHAR(255)          COMMENT '备注',
  created_at BIGINT       NOT NULL DEFAULT 0 COMMENT '登记时间（毫秒时间戳）',
  status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COVERED/INVALID',
  PRIMARY KEY (id),
  KEY idx_demand_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安装需求登记';

-- ---------------------------------------------------------------------------
-- 6. SLA 规则（时限类 / 速率类）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sla_rule (
  id              VARCHAR(32)  NOT NULL COMMENT '规则ID',
  order_type      VARCHAR(24)  NOT NULL COMMENT '业务类型 NEW_INSTALL/MOVE/REPAIR/SPEED_UP/RENEW',
  sla_name        VARCHAR(64)  NOT NULL COMMENT '规则名，如 新装-当日装',
  eval_type       VARCHAR(16)  NOT NULL COMMENT 'TIME=时限类 / SPEED=速率类',
  promised_hours  INT          NOT NULL DEFAULT 24 COMMENT '承诺完成小时数',
  grace_minutes   INT          NOT NULL DEFAULT 30 COMMENT '宽限分钟',
  min_speed_mbps  DOUBLE       NOT NULL DEFAULT 0 COMMENT '速率类最低达标速率',
  comp_type       VARCHAR(24)           COMMENT '赔付形式 VOUCHER/CASH/FEE_WAIVE',
  comp_amount     DOUBLE       NOT NULL DEFAULT 0 COMMENT '赔付基准金额/额度',
  comp_unit       VARCHAR(24)           COMMENT 'PER_ORDER / PER_OVERTIME_HOUR',
  max_comp_amount DOUBLE                COMMENT '赔付封顶（可空）',
  enabled         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (id),
  KEY idx_rule_type (order_type, eval_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA 规则';

-- ---------------------------------------------------------------------------
-- 7. SLA 评估记录
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sla_record (
  id               VARCHAR(32) NOT NULL COMMENT '记录ID',
  order_id         VARCHAR(32)          COMMENT '关联 work_order.id',
  order_type       VARCHAR(24)          COMMENT '业务类型',
  cust_name        VARCHAR(64)          COMMENT '客户名',
  rule_id          VARCHAR(32)          COMMENT '命中规则',
  accept_time      BIGINT      NOT NULL DEFAULT 0 COMMENT '受理时间（毫秒）',
  appointed_time   BIGINT      NOT NULL DEFAULT 0 COMMENT '预约上门时间（毫秒，可空取0）',
  complete_time    BIGINT               COMMENT '完工时间（毫秒，空=未完工）',
  speed_test_mbps  DOUBLE               COMMENT '装机测速结果（Mbps，空=未测速）',
  promised_time    BIGINT      NOT NULL DEFAULT 0 COMMENT '承诺完成时间（毫秒）',
  sla_status       VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/MET/OVERTIME',
  overtime_minutes INT         NOT NULL DEFAULT 0 COMMENT '超时分钟数',
  created_time     BIGINT      NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_sla_record_order (order_id),
  KEY idx_sla_record_status (sla_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA 评估记录';

-- ---------------------------------------------------------------------------
-- 8. 赔付工单
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS compensation (
  id            VARCHAR(32) NOT NULL COMMENT '赔付工单ID',
  sla_record_id VARCHAR(32)          COMMENT '关联 sla_record.id',
  order_id      VARCHAR(32)          COMMENT '关联工单ID',
  cust_name     VARCHAR(64)          COMMENT '客户名',
  order_type    VARCHAR(24)          COMMENT '业务类型',
  comp_type     VARCHAR(24)          COMMENT '赔付形式',
  comp_amount   DOUBLE      NOT NULL DEFAULT 0 COMMENT '赔付金额',
  reason        VARCHAR(255)         COMMENT '触发原因',
  status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/VERIFYING/PAID/REJECTED',
  created_time  BIGINT      NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_comp_status (status),
  KEY idx_comp_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赔付工单';

-- ---------------------------------------------------------------------------
-- 9. 套餐主表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS package_info (
  id          VARCHAR(32)  NOT NULL COMMENT '套餐ID',
  name        VARCHAR(128) NOT NULL COMMENT '套餐名称',
  category    VARCHAR(64)           COMMENT '分类：融合套餐 / 单宽带',
  monthly_fee INT          NOT NULL DEFAULT 0 COMMENT '月租（元）',
  original_fee INT         NOT NULL DEFAULT 0 COMMENT '原价（元）',
  status      VARCHAR(16)  NOT NULL DEFAULT 'ON_SHELF' COMMENT 'ON_SHELF/OFF_SHELF',
  deposit     INT          NOT NULL DEFAULT 0 COMMENT '调测费（一次性，元）',
  device_rent INT          NOT NULL DEFAULT 0 COMMENT '设备租赁（元/月）',
  penalty     VARCHAR(255)          COMMENT '违约金说明',
  sla_info    VARCHAR(255)          COMMENT '装维 SLA 说明',
  PRIMARY KEY (id),
  KEY idx_pkg_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐主表';

-- ---------------------------------------------------------------------------
-- 10. 套餐图片（主图 MAIN / 轮播 CAROUSEL / 详情 DETAIL）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS package_image (
  id         VARCHAR(32)  NOT NULL COMMENT '图片ID',
  package_id VARCHAR(32)  NOT NULL COMMENT '套餐ID',
  type       VARCHAR(16)  NOT NULL COMMENT 'MAIN/CAROUSEL/DETAIL',
  url        VARCHAR(512) NOT NULL COMMENT '图片地址',
  sort_order INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_pkg_image (package_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐图片';

-- ---------------------------------------------------------------------------
-- 11. 融合套餐组成项（宽带/手机/IPTV/副卡）
--     注意：列名用 description，接口字段为 desc（避免 MySQL 保留字 DESC）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS package_converge_item (
  id          VARCHAR(32)  NOT NULL COMMENT '组成项ID',
  package_id  VARCHAR(32)  NOT NULL COMMENT '套餐ID',
  label       VARCHAR(64)  NOT NULL COMMENT '组成名，如 宽带',
  description VARCHAR(255)          COMMENT '说明，如 500M 高速宽带',
  sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_converge_pkg (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融合套餐组成项';

-- ---------------------------------------------------------------------------
-- 12. 套餐动态参数组（单选 SINGLE / 多选 MULTI）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS package_param (
  id         VARCHAR(32) NOT NULL COMMENT '参数组ID',
  package_id VARCHAR(32) NOT NULL COMMENT '套餐ID',
  group_key  VARCHAR(64) NOT NULL COMMENT '业务键 bandwidth/contract/addon',
  name       VARCHAR(64) NOT NULL COMMENT '展示名，如 宽带速率',
  type       VARCHAR(16) NOT NULL COMMENT 'SINGLE/MULTI',
  required   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否必选',
  sort_order INT         NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_param_pkg (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐动态参数组';

-- ---------------------------------------------------------------------------
-- 13. 参数选项（extra_fee 为该选项相对套餐月租的加价）
--     注意：列名用 option_value，接口字段为 value
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS package_param_option (
  id           VARCHAR(32) NOT NULL COMMENT '选项ID',
  param_id     VARCHAR(32) NOT NULL COMMENT '参数组ID',
  option_value VARCHAR(64) NOT NULL COMMENT '选项值，如 500M',
  extra_fee    INT         NOT NULL DEFAULT 0 COMMENT '月加价（元/月）',
  sort_order   INT         NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_option_param (param_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐参数选项';

-- ---------------------------------------------------------------------------
-- 14. 流量用量（月度）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS traffic_usage (
  id              VARCHAR(32) NOT NULL COMMENT '记录ID',
  customer_id     VARCHAR(32) NOT NULL COMMENT '客户ID',
  period_month    VARCHAR(16) NOT NULL COMMENT '统计周期，如 2026-09',
  mobile_total    INT         NOT NULL DEFAULT 0 COMMENT '手机流量总量（G）',
  mobile_used     INT         NOT NULL DEFAULT 0 COMMENT '手机流量已用（G）',
  broadband_hours INT         NOT NULL DEFAULT 0 COMMENT '宽带当月时长（小时）',
  broadband_peak  VARCHAR(32)          COMMENT '宽带峰值速率，如 943M',
  daily_trend     VARCHAR(255)         COMMENT '近7日流量趋势（G，逗号分隔）',
  updated_at      DATETIME             COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_traffic_cust_month (customer_id, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量用量明细';

-- ---------------------------------------------------------------------------
-- 15. 客户（客户分层 / 关联套餐与小区）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer (
  id           VARCHAR(32)  NOT NULL COMMENT '客户ID',
  name         VARCHAR(64)  NOT NULL COMMENT '客户姓名',
  phone        VARCHAR(32)           COMMENT '联系电话',
  openid       VARCHAR(64)  DEFAULT NULL COMMENT '微信 openid（小程序登录绑定）',
  level        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '客户分层 NORMAL/SILVER/GOLD/VIP',
  package_id   VARCHAR(32)           COMMENT '当前套餐ID',
  community_id VARCHAR(32)           COMMENT '所属小区ID',
  address      VARCHAR(255)          COMMENT '安装地址',
  status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SUSPENDED/CLOSED',
  created_time BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_customer_phone (phone),
  KEY idx_customer_pkg (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

-- ---------------------------------------------------------------------------
-- 16. 客户合约（用于套餐升级剩余月数 / 补差折算）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_contract (
  id          VARCHAR(32) NOT NULL COMMENT '合约ID',
  customer_id VARCHAR(32) NOT NULL COMMENT '客户ID',
  package_id  VARCHAR(32) NOT NULL COMMENT '套餐ID',
  monthly_fee INT         NOT NULL DEFAULT 0 COMMENT '合约月费（元）',
  start_date  VARCHAR(10)          COMMENT '生效日 yyyy-MM-dd',
  end_date    VARCHAR(10)          COMMENT '到期日 yyyy-MM-dd',
  status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/TERMINATED',
  PRIMARY KEY (id),
  KEY idx_contract_cust (customer_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户合约';

-- ---------------------------------------------------------------------------
-- 17. 套餐升级申请单（PC 后台「套餐升级管理」数据源）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS package_upgrade_order (
  id              VARCHAR(32) NOT NULL COMMENT '申请单ID',
  customer_id     VARCHAR(32) NOT NULL COMMENT '客户ID',
  from_package_id VARCHAR(32)          COMMENT '原套餐ID',
  target_band_key VARCHAR(64)          COMMENT '目标带宽选项键',
  addon_keys      VARCHAR(255)         COMMENT '加购选项键（逗号分隔）',
  effect_type     VARCHAR(16)          COMMENT 'immediate / nextMonth',
  current_fee     INT         NOT NULL DEFAULT 0 COMMENT '原月费',
  month_diff      INT         NOT NULL DEFAULT 0 COMMENT '月补差',
  new_fee         INT         NOT NULL DEFAULT 0 COMMENT '新月费',
  one_time_diff   INT         NOT NULL DEFAULT 0 COMMENT '一次性补差',
  status          VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/EFFECTIVE/REJECTED',
  created_time    BIGINT      NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_upgrade_cust (customer_id),
  KEY idx_upgrade_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐升级申请单';

-- ---------------------------------------------------------------------------
-- 18. 小区表补列：覆盖运营商（PC 后台 小区覆盖管理 展示用）
--     MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，这里用 information_schema 判定后
--     动态执行，保证脚本可重复执行（幂等）。
-- ---------------------------------------------------------------------------
SET @add_carrier := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE community ADD COLUMN carrier VARCHAR(64) NULL COMMENT ''覆盖运营商''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'community' AND column_name = 'carrier'
);
PREPARE stmt_carrier FROM @add_carrier;
EXECUTE stmt_carrier;
DEALLOCATE PREPARE stmt_carrier;

-- ---------------------------------------------------------------------------
-- 20b. 安装工单补列：关联业务订单 biz_order_id（业务闭环追溯用）
-- ---------------------------------------------------------------------------
SET @add_wo_biz := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE work_order ADD COLUMN biz_order_id VARCHAR(32) NULL COMMENT ''关联业务订单 biz_order.id''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'work_order' AND column_name = 'biz_order_id'
);
PREPARE stmt_wo_biz FROM @add_wo_biz;
EXECUTE stmt_wo_biz;
DEALLOCATE PREPARE stmt_wo_biz;

-- ---------------------------------------------------------------------------
-- 19. 业务订单（PC 后台 订单管理 / 销售 / 财务 / 看板 数据源）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_order (
  id             VARCHAR(32)  NOT NULL COMMENT '订单号',
  customer_id    VARCHAR(32)           COMMENT '客户ID',
  customer_name  VARCHAR(64)           COMMENT '客户姓名',
  phone          VARCHAR(32)           COMMENT '联系电话',
  package_id     VARCHAR(32)           COMMENT '套餐ID',
  package_name   VARCHAR(128)          COMMENT '套餐名称',
  amount         INT          NOT NULL DEFAULT 0 COMMENT '订单金额（元）',
  sales_name     VARCHAR(64)           COMMENT '归属销售',
  community_id   VARCHAR(32)           COMMENT '小区ID',
  community_name VARCHAR(128)          COMMENT '小区名称',
  order_type     VARCHAR(24)  NOT NULL DEFAULT 'NEW_INSTALL' COMMENT 'NEW_INSTALL/MOVE/RENEW/SPEED_UP/REPAIR/ADDON',
  status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/INSTALLING/DONE/CANCELLED/REFUND',
  created_time   BIGINT       NOT NULL DEFAULT 0 COMMENT '下单时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_order_status (status),
  KEY idx_order_created (created_time),
  KEY idx_order_sales (sales_name),
  KEY idx_order_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单';

-- ---------------------------------------------------------------------------
-- 20. 投诉与评价（PC 后台 投诉与评价管理 数据源）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review (
  id            VARCHAR(32)  NOT NULL COMMENT '编号',
  order_id      VARCHAR(32)           COMMENT '关联订单/工单号',
  customer_name VARCHAR(64)           COMMENT '客户',
  worker_name   VARCHAR(64)           COMMENT '服务师傅',
  score         INT          NOT NULL DEFAULT 0 COMMENT '评分 0-5',
  tags          VARCHAR(255)          COMMENT '评价标签（逗号分隔）',
  type          VARCHAR(16)  NOT NULL DEFAULT 'REVIEW' COMMENT 'REVIEW=评价 / COMPLAINT=投诉',
  content       VARCHAR(512)          COMMENT '内容',
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/VISITED/CLOSED/TO_EVALUATE',
  created_time  BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_review_status (status),
  KEY idx_review_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉与评价';

-- ===========================================================================
-- RBAC（system 模块）：用户 / 角色 / 菜单权限 / 操作日志
-- 模型：用户(User) --< 用户角色 -- 角色(Role) --< 角色菜单 -- 菜单/权限(Menu)
-- 权限码（perm）同时是后端 @PreAuthorize 的 authority 与前端路由 meta.perm
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 21. 系统用户
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
  id           VARCHAR(32)  NOT NULL COMMENT '用户ID',
  username     VARCHAR(64)  NOT NULL COMMENT '登录账号',
  password     VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 哈希）',
  name         VARCHAR(64)  NOT NULL COMMENT '姓名',
  dept         VARCHAR(64)           COMMENT '部门',
  status       VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT '首次登录必须改密 0/1（默认管理员种子置 1）',
  created_time BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ---------------------------------------------------------------------------
-- 22. 角色
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
  id     VARCHAR(32) NOT NULL COMMENT '角色ID',
  code   VARCHAR(32) NOT NULL COMMENT '角色标识 ADMIN/OPERATOR/FINANCE/CS/SALES',
  name   VARCHAR(64) NOT NULL COMMENT '角色名称',
  remark VARCHAR(255)         COMMENT '说明',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- ---------------------------------------------------------------------------
-- 23. 菜单 / 权限（前端路由 meta.perm 与后端 authority 同源）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
  id         VARCHAR(32) NOT NULL COMMENT '菜单ID',
  parent_id  VARCHAR(32)          COMMENT '父级ID（顶级为空）',
  name       VARCHAR(64) NOT NULL COMMENT '名称',
  path       VARCHAR(128)         COMMENT '前端路由',
  perm       VARCHAR(64)          COMMENT '权限码，如 order:view',
  type       VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT 'DIR=目录 / MENU=菜单 / BUTTON=按钮',
  sort_order INT         NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单与权限';

-- ---------------------------------------------------------------------------
-- 24. 用户-角色
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
  id      BIGINT      NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
  role_id VARCHAR(32) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ---------------------------------------------------------------------------
-- 25. 角色-菜单（角色授权）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu (
  id      BIGINT      NOT NULL AUTO_INCREMENT,
  role_id VARCHAR(32) NOT NULL COMMENT '角色ID',
  menu_id VARCHAR(32) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- ---------------------------------------------------------------------------
-- 26. 操作日志（登录、写操作、越权尝试）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  username     VARCHAR(64)          COMMENT '账号',
  name         VARCHAR(64)          COMMENT '姓名',
  action       VARCHAR(64)          COMMENT '操作',
  target       VARCHAR(255)         COMMENT '目标接口',
  method       VARCHAR(16)          COMMENT 'HTTP 方法',
  ip           VARCHAR(64)          COMMENT '来源IP',
  result       VARCHAR(32)          COMMENT '结果：成功 / 拒绝(403) / 未认证(401) / 失败',
  cost_ms      BIGINT      NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
  created_time BIGINT      NOT NULL DEFAULT 0 COMMENT '时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_log_created (created_time),
  KEY idx_log_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ---------------------------------------------------------------------------
-- 27. 部门（按区域划分：华南大区 > 深圳分公司 / 广州分公司；华东大区 > 上海分公司）
--     树形结构：parent_id 为空表示区域根节点；sys_user / community / 订单 通过 dept_id 归属部门，
--     用于「运营人员只能看到本部门及下级部门数据」的数据权限（行级隔离）。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_department (
  id           VARCHAR(32)  NOT NULL COMMENT '部门ID',
  parent_id    VARCHAR(32)           COMMENT '父部门ID（区域树，顶级为空）',
  name         VARCHAR(64)  NOT NULL COMMENT '部门名称',
  region       VARCHAR(32)           COMMENT '所属区域（华南/华东/华北...）',
  sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status       VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  created_time BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_dept_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门（按区域划分）';

-- ---------------------------------------------------------------------------
-- 27b. 历史列改造 + 各业务表补 dept_id（幂等）
--     MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，沿用 information_schema 守卫。
-- ---------------------------------------------------------------------------

-- sys_user：dept 自由文本 -> dept_id（关联 sys_department）
SET @drop_u_dept := (
  SELECT IF(COUNT(*) = 0, 'SELECT 1',
            'ALTER TABLE sys_user DROP COLUMN dept')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'dept'
);
PREPARE stmt_drop_u_dept FROM @drop_u_dept;
EXECUTE stmt_drop_u_dept;
DEALLOCATE PREPARE stmt_drop_u_dept;

SET @add_u_dept := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE sys_user ADD COLUMN dept_id VARCHAR(32) NULL COMMENT ''部门ID（关联 sys_department）''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'dept_id'
);
PREPARE stmt_u_dept FROM @add_u_dept;
EXECUTE stmt_u_dept;
DEALLOCATE PREPARE stmt_u_dept;

-- community：归属部门
SET @add_c_dept := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE community ADD COLUMN dept_id VARCHAR(32) NULL COMMENT ''归属部门（按区域划分）''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'community' AND column_name = 'dept_id'
);
PREPARE stmt_c_dept FROM @add_c_dept;
EXECUTE stmt_c_dept;
DEALLOCATE PREPARE stmt_c_dept;

-- biz_order：归属部门
SET @add_o_dept := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE biz_order ADD COLUMN dept_id VARCHAR(32) NULL COMMENT ''归属部门（按区域划分）'', ADD KEY idx_biz_dept (dept_id)',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_order' AND column_name = 'dept_id'
);
PREPARE stmt_o_dept FROM @add_o_dept;
EXECUTE stmt_o_dept;
DEALLOCATE PREPARE stmt_o_dept;

-- biz_order：所属片区（销售页「负责片区」；由 community 推导，幂等）
SET @add_o_region := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE biz_order ADD COLUMN region VARCHAR(64) NULL COMMENT ''所属片区（由 community 推导，用于销售业绩归属）''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_order' AND column_name = 'region'
);
PREPARE stmt_o_region FROM @add_o_region;
EXECUTE stmt_o_region;
DEALLOCATE PREPARE stmt_o_region;

-- work_order：归属部门
SET @add_w_dept := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE work_order ADD COLUMN dept_id VARCHAR(32) NULL COMMENT ''归属部门（按区域划分）'', ADD KEY idx_wo_dept (dept_id)',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'work_order' AND column_name = 'dept_id'
);
PREPARE stmt_w_dept FROM @add_w_dept;
EXECUTE stmt_w_dept;
DEALLOCATE PREPARE stmt_w_dept;

-- ---------------------------------------------------------------------------
-- 28b. T-02 安全治理：sys_user 增加「首次登录必须改密」标记（幂等）
--   新库由上面 CREATE TABLE 直接带列；此处仅对「已有 sys_user 表但缺该列」的老库补齐。
-- ---------------------------------------------------------------------------
SET @add_u_mcp := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_user ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT ''首次登录必须改密 0/1''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'must_change_password'
);
PREPARE stmt_u_mcp FROM @add_u_mcp;
EXECUTE stmt_u_mcp;
DEALLOCATE PREPARE stmt_u_mcp;

-- ============================================================================
-- 退款工单（退款 / 对账状态机）
--   biz_order.status = 'REFUND' 表示已退款（财务对账口径：营收扣减）。
--   状态机：PENDING(客户申请) -> APPROVED(财务通过) -> REFUNDED(已退款) / REJECTED(驳回)
-- ============================================================================
CREATE TABLE IF NOT EXISTS order_refund (
  id            VARCHAR(32)  NOT NULL COMMENT '退款单号',
  order_id      VARCHAR(32)  NOT NULL COMMENT '关联业务订单号',
  order_no      VARCHAR(32)           COMMENT '业务订单号（冗余，便于查询）',
  customer_id   VARCHAR(32)           COMMENT '客户ID',
  customer_name VARCHAR(64)           COMMENT '客户姓名',
  amount        INT          NOT NULL DEFAULT 0 COMMENT '退款金额（元）',
  reason        VARCHAR(255)          COMMENT '退款原因',
  channel       VARCHAR(32)  NOT NULL DEFAULT 'WECHAT_MOCK' COMMENT '退款渠道（占位：真实接入后填微信支付退款单号）',
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/REFUNDED',
  refund_no     VARCHAR(64)           COMMENT '第三方退款流水号',
  operator      VARCHAR(64)           COMMENT '处理人',
  created_time  BIGINT       NOT NULL DEFAULT 0 COMMENT '申请时间（毫秒）',
  handled_time  BIGINT                COMMENT '处理时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_refund_order (order_id),
  KEY idx_refund_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款工单';

-- ============================================================================
-- 电子发票申请（占位）
--   状态机：PENDING(客户申请) -> OPENED(已开具) / REJECTED(驳回)
--   pdf_url 为占位地址，真实接入电子发票平台后回填。
-- ============================================================================
CREATE TABLE IF NOT EXISTS invoice_apply (
  id            VARCHAR(32)  NOT NULL COMMENT '申请单号',
  order_id      VARCHAR(32)  NOT NULL COMMENT '关联业务订单号',
  order_no      VARCHAR(32)           COMMENT '业务订单号（冗余）',
  customer_id   VARCHAR(32)           COMMENT '客户ID',
  customer_name VARCHAR(64)           COMMENT '客户姓名',
  title         VARCHAR(128) NOT NULL COMMENT '发票抬头',
  tax_no        VARCHAR(64)           COMMENT '税号（企业抬头必填）',
  amount        INT          NOT NULL DEFAULT 0 COMMENT '开票金额（元）',
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/OPENED/REJECTED',
  invoice_no    VARCHAR(64)           COMMENT '发票号码',
  pdf_url       VARCHAR(255)          COMMENT '电子发票 PDF 地址（占位）',
  operator      VARCHAR(64)           COMMENT '开票员',
  remark        VARCHAR(255)          COMMENT '驳回原因 / 备注',
  created_time  BIGINT       NOT NULL DEFAULT 0 COMMENT '申请时间（毫秒）',
  opened_time   BIGINT                COMMENT '开票时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_invoice_order (order_id),
  KEY idx_invoice_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子发票申请';

-- invoice_apply：补齐 customer_id（历史库可能由更早的 schema 创建而缺此列）
SET @add_inv_cid := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE invoice_apply ADD COLUMN customer_id VARCHAR(32) NULL COMMENT ''客户ID''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'invoice_apply' AND column_name = 'customer_id'
);
PREPARE stmt_inv_cid FROM @add_inv_cid;
EXECUTE stmt_inv_cid;
DEALLOCATE PREPARE stmt_inv_cid;

-- ============================================================================
-- 28. 数据字典类型（sys_dict_type）
--     与 sys_dict_data 配合，替代散落在代码中的硬编码枚举（故障类型 / 工单类型 / 时段等）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
  dict_type   VARCHAR(64)  NOT NULL COMMENT '字典类型编码，如 fault_category',
  dict_name   VARCHAR(64)  NOT NULL COMMENT '字典类型名称，如 故障类型',
  status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  remark      VARCHAR(255)          COMMENT '备注',
  create_time BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型';

-- ============================================================================
-- 29. 数据字典数据项（sys_dict_data）
-- ============================================================================
CREATE TABLE IF NOT EXISTS sys_dict_data (
  id          VARCHAR(32)  NOT NULL COMMENT '数据项ID',
  dict_type   VARCHAR(64)  NOT NULL COMMENT '关联 sys_dict_type.dict_type',
  dict_label  VARCHAR(128) NOT NULL COMMENT '展示标签，如 网络中断',
  dict_value  VARCHAR(128) NOT NULL COMMENT '值，如 NETWORK_DOWN',
  dict_sort   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  remark      VARCHAR(255)          COMMENT '备注',
  create_time BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_dict_data_type (dict_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典数据项';

-- ============================================================================
-- 30. 参数配置（sys_config）
--     系统级可配置参数（客服电话 / 报修承诺时长 / 派单默认容量等），后台可在线维护。
-- ============================================================================
CREATE TABLE IF NOT EXISTS sys_config (
  config_key   VARCHAR(64)  NOT NULL COMMENT '参数键，如 repair.sla.promised.hours',
  config_name  VARCHAR(128) NOT NULL COMMENT '参数名称',
  config_value VARCHAR(512) NOT NULL DEFAULT '' COMMENT '参数值',
  config_type  VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT 'STRING/INT/BOOLEAN/JSON',
  remark       VARCHAR(255)          COMMENT '备注',
  create_time  BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数配置';

-- ============================================================================
-- 31. 安装工单补列：工单类型 + 故障报修字段（V1.13 故障报修全流程）
--     MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，沿用 information_schema 守卫，保证幂等。
-- ============================================================================

-- work_order.type：INSTALL(新装) / REPAIR(故障报修) / MOVE(移机) / SPEED_UP(提速) / RENEW(续费)
SET @add_wo_type := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE work_order ADD COLUMN type VARCHAR(16) NOT NULL DEFAULT ''INSTALL'' COMMENT ''工单类型 INSTALL/REPAIR/MOVE/SPEED_UP/RENEW''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'work_order' AND column_name = 'type'
);
PREPARE stmt_wo_type FROM @add_wo_type;
EXECUTE stmt_wo_type;
DEALLOCATE PREPARE stmt_wo_type;

SET @add_wo_fc := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE work_order ADD COLUMN fault_category VARCHAR(64) NULL COMMENT ''故障类型（报修用，关联数据字典 fault_category）''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'work_order' AND column_name = 'fault_category'
);
PREPARE stmt_wo_fc FROM @add_wo_fc;
EXECUTE stmt_wo_fc;
DEALLOCATE PREPARE stmt_wo_fc;

SET @add_wo_fd := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE work_order ADD COLUMN fault_desc VARCHAR(255) NULL COMMENT ''故障描述（报修用）''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'work_order' AND column_name = 'fault_desc'
);
PREPARE stmt_wo_fd FROM @add_wo_fd;
EXECUTE stmt_wo_fd;
DEALLOCATE PREPARE stmt_wo_fd;

SET @add_wo_cp := (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE work_order ADD COLUMN contact_phone VARCHAR(32) NULL COMMENT ''报修联系电话（报修用，可区别于客户登记号）''',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'work_order' AND column_name = 'contact_phone'
);
PREPARE stmt_wo_cp FROM @add_wo_cp;
EXECUTE stmt_wo_cp;
DEALLOCATE PREPARE stmt_wo_cp;

-- ============================================================================
-- 32~38. V1.14 运营与留存：积分成长 / 优惠活动 / 在线客服
-- ============================================================================

-- 32. 积分账户（一个客户一条）
CREATE TABLE IF NOT EXISTS points_account (
  customer_id   VARCHAR(32)  NOT NULL COMMENT '客户ID（PK）',
  balance       INT          NOT NULL DEFAULT 0 COMMENT '当前积分',
  total_earned  INT          NOT NULL DEFAULT 0 COMMENT '累计获得',
  total_spent   INT          NOT NULL DEFAULT 0 COMMENT '累计消耗',
  sign_date     VARCHAR(10)           COMMENT '最近签到日期 yyyy-MM-dd',
  sign_streak   INT          NOT NULL DEFAULT 0 COMMENT '连续签到天数',
  created_time  BIGINT       NOT NULL DEFAULT 0 COMMENT '创建时间（毫秒）',
  PRIMARY KEY (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分账户';

-- 33. 积分流水
CREATE TABLE IF NOT EXISTS points_record (
  id           VARCHAR(32)  NOT NULL COMMENT '流水ID',
  customer_id  VARCHAR(32)  NOT NULL COMMENT '客户ID',
  type         VARCHAR(16)  NOT NULL COMMENT 'SIGN/TASK/REDEEM/EXPIRE/ADJUST',
  amount       INT          NOT NULL DEFAULT 0 COMMENT '积分变动（正=获得，负=消耗）',
  remark       VARCHAR(255)          COMMENT '说明',
  ref_id       VARCHAR(32)           COMMENT '关联单号（优惠券/订单）',
  created_time BIGINT       NOT NULL DEFAULT 0 COMMENT '时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_pr_cust (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- 34. 积分任务（签到/完善资料/首评/邀请好友）
CREATE TABLE IF NOT EXISTS points_task (
  id           VARCHAR(32)  NOT NULL COMMENT '任务ID',
  task_key     VARCHAR(32)  NOT NULL COMMENT '任务键 signin/profile/first_review/invite',
  name         VARCHAR(64)  NOT NULL COMMENT '任务名',
  points       INT          NOT NULL DEFAULT 0 COMMENT '奖励积分',
  description  VARCHAR(255)          COMMENT '说明',
  sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  status       VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  PRIMARY KEY (id),
  KEY idx_pt_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分任务';

-- 35. 积分商城商品（兑换项）
CREATE TABLE IF NOT EXISTS points_mall_item (
  id           VARCHAR(32)  NOT NULL COMMENT '商品ID',
  name         VARCHAR(128) NOT NULL COMMENT '商品名',
  cost         INT          NOT NULL DEFAULT 0 COMMENT '兑换所需积分',
  stock        INT          NOT NULL DEFAULT -1 COMMENT '库存（-1 表示不限）',
  coupon_type  VARCHAR(32)  NOT NULL DEFAULT 'SPEED_UP' COMMENT 'SPEED_UP/VOUCHER/PHYSICAL',
  coupon_value VARCHAR(64)           COMMENT '券面额/说明（如 提速至500M / 10元现金券 / 实物描述）',
  image        VARCHAR(512)          COMMENT '图片',
  status       VARCHAR(16)  NOT NULL DEFAULT 'ON_SHELF' COMMENT 'ON_SHELF/OFF_SHELF',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商城商品';

-- 36. 兑换得到的优惠券
CREATE TABLE IF NOT EXISTS points_coupon (
  id           VARCHAR(32)  NOT NULL COMMENT '券ID',
  customer_id  VARCHAR(32)  NOT NULL COMMENT '客户ID',
  item_id      VARCHAR(32)           COMMENT '来源商品ID',
  coupon_code  VARCHAR(64)  NOT NULL COMMENT '券码',
  coupon_type  VARCHAR(32)           COMMENT 'SPEED_UP/VOUCHER/PHYSICAL',
  coupon_value VARCHAR(64)           COMMENT '券面额/说明',
  status       VARCHAR(16)  NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
  created_time BIGINT       NOT NULL DEFAULT 0 COMMENT '兑换时间（毫秒）',
  expire_time  BIGINT                COMMENT '过期时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_pc_cust (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换优惠券';

-- 37. 优惠活动专区
CREATE TABLE IF NOT EXISTS promotion (
  id           VARCHAR(32)  NOT NULL COMMENT '活动ID',
  title        VARCHAR(128) NOT NULL COMMENT '活动标题',
  subtitle     VARCHAR(255)          COMMENT '副标题',
  cover        VARCHAR(512)          COMMENT '封面图',
  type         VARCHAR(32)  NOT NULL DEFAULT 'COMBO' COMMENT 'ANNUAL/NEW_INSTALL/COMBO/LIMITED',
  target       VARCHAR(64)           COMMENT '适用套餐ID（ALL 表示通用）',
  start_date   VARCHAR(10)          COMMENT '开始 yyyy-MM-dd',
  end_date     VARCHAR(10)          COMMENT '结束 yyyy-MM-dd',
  rule_json    VARCHAR(1024)        COMMENT '优惠规则 JSON（如 买12送2 / 直降100）',
  status       VARCHAR(16)  NOT NULL DEFAULT 'ONLINE' COMMENT 'ONLINE/OFFLINE',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠活动';

-- 38. 在线客服 FAQ
CREATE TABLE IF NOT EXISTS support_faq (
  id           VARCHAR(32)  NOT NULL COMMENT 'FAQ ID',
  category     VARCHAR(32)  NOT NULL DEFAULT 'GENERAL' COMMENT '网络/账单/报修/账户',
  question     VARCHAR(255) NOT NULL COMMENT '问题',
  answer       VARCHAR(1024)         COMMENT '回答',
  sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线客服FAQ';

-- 39. 在线客服工单（工单式咨询）
CREATE TABLE IF NOT EXISTS support_ticket (
  id            VARCHAR(32)  NOT NULL COMMENT '工单ID',
  customer_id   VARCHAR(32)           COMMENT '客户ID',
  customer_name VARCHAR(64)           COMMENT '客户名',
  type          VARCHAR(32)  NOT NULL DEFAULT 'CONSULT' COMMENT 'CONSULT/FAULT/COMPLAINT',
  content       VARCHAR(1024)         COMMENT '咨询内容',
  contact       VARCHAR(32)           COMMENT '联系方式',
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REPLIED/CLOSED',
  reply         VARCHAR(1024)         COMMENT '回复内容',
  created_time  BIGINT       NOT NULL DEFAULT 0 COMMENT '提交时间（毫秒）',
  PRIMARY KEY (id),
  KEY idx_st_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线客服工单';
