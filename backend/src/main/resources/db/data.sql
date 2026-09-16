-- ============================================================================
-- 宽带业务管理系统 · 初始化种子数据（data.sql）
-- 全部使用 INSERT IGNORE + 显式主键 => 幂等，重复启动不会重复插入，
-- 也不会覆盖运行期新增/修改的数据。手动执行：mysql -uroot -p broadband < data.sql
-- 说明：'demo' 为演示客户账号（对应小程序 customerId=demo）。
-- ============================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 小区（南山科技园片区 + 麒麟 + 福田香蜜湖；相邻性由街道/经纬度判定）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO community (id, name, region, street, latitude, longitude, installable, port_total, port_used) VALUES
 ('com_ns01', '科技园小区',   '南山', '科技园路', 22.5400, 113.9400, 1, 240, 168),
 ('com_ns02', '深大新村',     '南山', '科技园路', 22.5410, 113.9410, 1, 180,  96),
 ('com_ns03', '麒麟花园',     '南山', '麒麟路',   22.5600, 113.9700, 1, 120,  44),
 ('com_ft01', '香蜜湖小区',   '福田', '香蜜湖路', 22.5450, 114.0300, 1, 300, 210),
 ('com_ba01', '宝安中心花园', '宝安', '宝安大道', 22.5600, 113.8800, 0,   0,   0);

-- ---------------------------------------------------------------------------
-- 安装师傅
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO worker (id, name, region, skill_level, phone) VALUES
 ('w01', '张伟', '南山', 3, '13700000001'),
 ('w02', '李强', '福田', 2, '13700000002'),
 ('w03', '王芳', '南山', 1, '13700000003');

-- ---------------------------------------------------------------------------
-- 师傅时段容量（覆盖默认策略：相邻 3-4 / 非相邻 1-2）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO worker_capacity (id, worker_id, time_slot, adjacent_cap, non_adjacent_cap) VALUES
 (1, 'w01', '2026-09-15#AM', 4, 2),
 (2, 'w02', '2026-09-15#AM', 4, 2),
 (3, 'w03', '2026-09-15#AM', 3, 1);

-- ---------------------------------------------------------------------------
-- 待派工单（时段 2026-09-15#AM）：相邻簇 4+3=7 单，非相邻 2+1=3 单
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO work_order (id, community_id, address, time_slot, customer_name, package_desc, status, adjacent_route) VALUES
 ('WO2026091501', 'com_ns01', '科技园路1号 3栋802', '2026-09-15#AM', '赵敏', '500M 融合套餐', 'PENDING', NULL),
 ('WO2026091502', 'com_ns01', '科技园路1号 5栋1101', '2026-09-15#AM', '钱勇', '1000M 融合套餐', 'PENDING', NULL),
 ('WO2026091503', 'com_ns01', '科技园路3号 1栋201', '2026-09-15#AM', '孙丽', '300M 单宽带', 'PENDING', NULL),
 ('WO2026091504', 'com_ns01', '科技园路3号 2栋702', '2026-09-15#AM', '李强', '500M 融合套餐', 'PENDING', NULL),
 ('WO2026091505', 'com_ns02', '科技园路9号 8栋601', '2026-09-15#AM', '周涛', '500M 融合套餐', 'PENDING', NULL),
 ('WO2026091506', 'com_ns02', '科技园路9号 9栋302', '2026-09-15#AM', '吴迪', '1000M 融合套餐', 'PENDING', NULL),
 ('WO2026091507', 'com_ns02', '科技园路11号 2栋501', '2026-09-15#AM', '郑爽', '300M 单宽带', 'PENDING', NULL),
 ('WO2026091508', 'com_ns03', '麒麟路20号 6栋101', '2026-09-15#AM', '冯磊', '500M 融合套餐', 'PENDING', NULL),
 ('WO2026091509', 'com_ns03', '麒麟路22号 3栋902', '2026-09-15#AM', '陈晨', '1000M 融合套餐', 'PENDING', NULL),
 ('WO2026091510', 'com_ft01', '香蜜湖路88号 1栋1601', '2026-09-15#AM', '褚辉', '500M 融合套餐', 'PENDING', NULL);

-- ---------------------------------------------------------------------------
-- 未覆盖小区安装需求登记
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO community_demand (id, name, contact, phone, note, created_at, status) VALUES
 ('dm20260901001', '宝安中心花园', '刘先生', '13900001111', '小区 3 期希望尽快开通覆盖', UNIX_TIMESTAMP('2026-09-01 10:20:00') * 1000, 'PENDING');

-- ---------------------------------------------------------------------------
-- SLA 规则（参考电信「当日装当日修 / 慢必赔」、移动「超时赔 / 网速不达标赔」）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sla_rule (id, order_type, sla_name, eval_type, promised_hours, grace_minutes, min_speed_mbps, comp_type, comp_amount, comp_unit, max_comp_amount, enabled) VALUES
 ('R_NEW_TIME',     'NEW_INSTALL', '新装-当日装',        'TIME',  24, 30, 0,   'VOUCHER', 5,  'PER_OVERTIME_HOUR', 20,   1),
 ('R_REPAIR_TIME',  'REPAIR',      '报修-当日修',        'TIME',  24, 30, 0,   'CASH',    20, 'PER_ORDER',         50,   1),
 ('R_MOVE_TIME',    'MOVE',        '移机-当日移',        'TIME',  24, 30, 0,   'CASH',    20, 'PER_ORDER',         50,   1),
 ('R_INSTALL_SPEED','NEW_INSTALL', '装机-网速达标≥500M', 'SPEED',  0,  0, 500, 'CASH',    20, 'PER_ORDER',         NULL, 1);

-- ---------------------------------------------------------------------------
-- SLA 评估记录（含达标 / 超时 / 速率不达标，供看板聚合）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sla_record (id, order_id, order_type, cust_name, rule_id, accept_time, appointed_time, complete_time, speed_test_mbps, promised_time, sla_status, overtime_minutes, created_time) VALUES
 ('SR20260910001', 'WO2026091001', 'NEW_INSTALL', '王女士', 'R_NEW_TIME',
  UNIX_TIMESTAMP('2026-09-10 09:00:00') * 1000, 0,
  UNIX_TIMESTAMP('2026-09-10 20:30:00') * 1000, NULL,
  (UNIX_TIMESTAMP('2026-09-10 09:00:00') + 24 * 3600 - 30 * 60) * 1000, 'MET', 0,
  UNIX_TIMESTAMP('2026-09-10 20:30:00') * 1000),

 ('SR20260911001', 'WO2026091101', 'NEW_INSTALL', '陈先生', 'R_NEW_TIME',
  UNIX_TIMESTAMP('2026-09-11 10:00:00') * 1000, 0,
  UNIX_TIMESTAMP('2026-09-12 14:00:00') * 1000, NULL,
  (UNIX_TIMESTAMP('2026-09-11 10:00:00') + 24 * 3600 - 30 * 60) * 1000, 'OVERTIME', 270,
  UNIX_TIMESTAMP('2026-09-12 14:00:00') * 1000),

 ('SR20260912001', 'WO2026091201', 'NEW_INSTALL', '刘先生', 'R_INSTALL_SPEED',
  UNIX_TIMESTAMP('2026-09-12 09:00:00') * 1000, 0,
  UNIX_TIMESTAMP('2026-09-12 12:00:00') * 1000, 380,
  0, 'OVERTIME', 0,
  UNIX_TIMESTAMP('2026-09-12 12:00:00') * 1000),

 ('SR20260913001', 'WO2026091301', 'REPAIR', '赵女士', 'R_REPAIR_TIME',
  UNIX_TIMESTAMP('2026-09-13 08:00:00') * 1000, 0,
  UNIX_TIMESTAMP('2026-09-13 09:00:00') * 1000, NULL,
  (UNIX_TIMESTAMP('2026-09-13 08:00:00') + 24 * 3600 - 30 * 60) * 1000, 'MET', 0,
  UNIX_TIMESTAMP('2026-09-13 09:00:00') * 1000);

-- ---------------------------------------------------------------------------
-- 赔付工单
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO compensation (id, sla_record_id, order_id, cust_name, order_type, comp_type, comp_amount, reason, status, created_time) VALUES
 ('CP20260912001', 'SR20260911001', 'WO2026091101', '陈先生', 'NEW_INSTALL', 'VOUCHER', 20,
  '新装-当日装 超时 270 分钟触发慢必赔', 'PENDING', UNIX_TIMESTAMP('2026-09-12 14:05:00') * 1000),
 ('CP20260912002', 'SR20260912001', 'WO2026091201', '刘先生', 'NEW_INSTALL', 'CASH', 20,
  '装机-网速达标≥500M 装机测速 380Mbps 未达 500Mbps，触发赔付', 'PENDING', UNIX_TIMESTAMP('2026-09-12 12:05:00') * 1000);

-- ---------------------------------------------------------------------------
-- 套餐主表
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO package_info (id, name, category, monthly_fee, original_fee, status, deposit, device_rent, penalty, sla_info) VALUES
 ('pkg500',  '500M 融合套餐',  '融合套餐',  99, 129, 'ON_SHELF', 100, 10, '合约期内提前解约需支付剩余月费 30% 作为违约金', '城区当日装当日修，超时或网速不达标按 SLA 自动赔付'),
 ('pkg1000', '1000M 融合套餐', '融合套餐', 159, 199, 'ON_SHELF', 100, 10, '合约期内提前解约需支付剩余月费 30% 作为违约金', '城区当日装当日修，超时或网速不达标按 SLA 自动赔付'),
 ('pkg300',  '300M 单宽带',    '单宽带',    69,  89, 'ON_SHELF', 100,  0, '合约期内提前解约需支付剩余月费 30% 作为违约金', '城区当日装当日修，超时按 SLA 自动赔付');

-- ---------------------------------------------------------------------------
-- 套餐图片（pkg500：1 主图 + 2 轮播 + 1 详情）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO package_image (id, package_id, type, url, sort_order) VALUES
 ('img500m', 'pkg500', 'MAIN',     '/assets/pkg500-main.png',   1),
 ('img500c1','pkg500', 'CAROUSEL', '/assets/pkg500-caro-1.png', 1),
 ('img500c2','pkg500', 'CAROUSEL', '/assets/pkg500-caro-2.png', 2),
 ('img500d1','pkg500', 'DETAIL',   '/assets/pkg500-detail.png', 1);

-- ---------------------------------------------------------------------------
-- 融合套餐组成项（列 description 对应接口字段 desc）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO package_converge_item (id, package_id, label, description, sort_order) VALUES
 ('cv500_1', 'pkg500', '宽带', '500M 高速光纤宽带',       1),
 ('cv500_2', 'pkg500', '手机', '30GB 流量 + 500 分钟通话', 2),
 ('cv500_3', 'pkg500', 'IPTV', '4K 超清电视',             3),
 ('cv500_4', 'pkg500', '副卡', '2 张共享副卡',            4);

-- ---------------------------------------------------------------------------
-- 套餐动态参数组
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO package_param (id, package_id, group_key, name, type, required, sort_order) VALUES
 ('pp500_bw',    'pkg500', 'bandwidth', '宽带速率', 'SINGLE', 1, 1),
 ('pp500_ct',    'pkg500', 'contract',  '合约期',   'SINGLE', 1, 2),
 ('pp500_addon', 'pkg500', 'addon',     '增值服务', 'MULTI',  0, 3);

-- ---------------------------------------------------------------------------
-- 参数选项（列 option_value 对应接口字段 value）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO package_param_option (id, param_id, option_value, extra_fee, sort_order) VALUES
 ('po500_bw300',  'pp500_bw', '300M',  0, 1),
 ('po500_bw500',  'pp500_bw', '500M',  0, 2),
 ('po500_bw1000', 'pp500_bw', '1000M', 40, 3),
 ('po500_bw2000', 'pp500_bw', '2000M', 90, 4),
 ('po500_ct12',   'pp500_ct', '12 个月', 0, 1),
 ('po500_ct24',   'pp500_ct', '24 个月', 0, 2),
 ('po500_ad_fttr','pp500_addon', 'FTTR 全屋光纤', 30, 1),
 ('po500_ad_wifi','pp500_addon', '全屋 WiFi',     15, 2),
 ('po500_ad_see', 'pp500_addon', '看家',          10, 3);

-- ---------------------------------------------------------------------------
-- 客户（demo 为演示账号，对应小程序 customerId=demo）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO customer (id, name, phone, level, package_id, community_id, address, status, created_time) VALUES
 ('demo',    '演示客户', '13800000000', 'GOLD',   'pkg500',  'com_ns01', '深圳市南山区科技园路1号 3栋802',   'ACTIVE', UNIX_TIMESTAMP('2026-04-01 10:00:00') * 1000),
 ('C20260002', '林小雨', '13800000002', 'SILVER', 'pkg1000', 'com_ns02', '深圳市南山区科技园路9号 8栋601',   'ACTIVE', UNIX_TIMESTAMP('2026-05-12 14:30:00') * 1000),
 ('C20260003', '何大军', '13800000003', 'NORMAL', 'pkg300',  'com_ft01', '深圳市福田区香蜜湖路88号 1栋1601','ACTIVE', UNIX_TIMESTAMP('2026-06-20 09:15:00') * 1000);

-- ---------------------------------------------------------------------------
-- 客户合约（demo 剩余约 18 个月，用于升级补差折算）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO customer_contract (id, customer_id, package_id, monthly_fee, start_date, end_date, status) VALUES
 ('ct_demo',   'demo',      'pkg500',   99, '2026-04-01', '2028-03-31', 'ACTIVE'),
 ('ct_c0002',  'C20260002', 'pkg1000', 159, '2026-05-12', '2028-05-11', 'ACTIVE'),
 ('ct_c0003',  'C20260003', 'pkg300',   69, '2026-06-20', '2027-06-19', 'ACTIVE');

-- ---------------------------------------------------------------------------
-- 流量用量（当期 2026-09）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO traffic_usage (id, customer_id, period_month, mobile_total, mobile_used, broadband_hours, broadband_peak, daily_trend, updated_at) VALUES
 ('tu_demo_202609',  'demo',      '2026-09', 30, 22, 186, '943M',  '3,2,4,1,3,2,5', NOW()),
 ('tu_c0002_202609', 'C20260002', '2026-09', 60, 51, 268, '961M',  '6,5,7,4,8,6,7', NOW()),
 ('tu_c0003_202609', 'C20260003', '2026-09', 20,  9,  92, '312M',  '1,2,1,3,2,1,2', NOW());

-- ---------------------------------------------------------------------------
-- 小区覆盖运营商（第 2 轮新增列；此处补齐种子值）
-- ---------------------------------------------------------------------------
UPDATE community SET carrier = '电信·联通' WHERE id IN ('com_ns01','com_ns02') AND (carrier IS NULL OR carrier = '');
UPDATE community SET carrier = '电信'      WHERE id = 'com_ns03' AND (carrier IS NULL OR carrier = '');
UPDATE community SET carrier = '联通'      WHERE id = 'com_ft01' AND (carrier IS NULL OR carrier = '');
UPDATE community SET carrier = '—'         WHERE id = 'com_ba01' AND (carrier IS NULL OR carrier = '');

-- ---------------------------------------------------------------------------
-- 业务订单（近 3 个月，供 订单管理 / 销售业绩 / 财务对账 / 数据看板 聚合）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO biz_order (id, customer_id, customer_name, phone, package_id, package_name, amount, sales_name, community_id, community_name, order_type, status, created_time) VALUES
 -- 2026-07
 ('B20260701001','C20260002','林小雨','13800000002','pkg1000','1000M 融合套餐',159,'刘伟','com_ns02','深大新村','NEW_INSTALL','DONE',    UNIX_TIMESTAMP('2026-07-03 10:20:00')*1000),
 ('B20260701002','C20260003','何大军','13800000003','pkg300', '300M 单宽带',    69,'赵敏','com_ft01','香蜜湖小区','NEW_INSTALL','DONE',  UNIX_TIMESTAMP('2026-07-11 15:40:00')*1000),
 ('B20260701003','demo',     '演示客户','13800000000','pkg500','500M 融合套餐', 99,'刘伟','com_ns01','科技园小区','RENEW',       'DONE',  UNIX_TIMESTAMP('2026-07-22 09:05:00')*1000),
 -- 2026-08
 ('B20260801001','C20260002','林小雨','13800000002','pkg1000','1000M 融合套餐',159,'王芳','com_ns02','深大新村','SPEED_UP','DONE',      UNIX_TIMESTAMP('2026-08-05 11:12:00')*1000),
 ('B20260801002','C20260003','何大军','13800000003','pkg300', '300M 单宽带',    69,'赵敏','com_ft01','香蜜湖小区','REPAIR',   'DONE',    UNIX_TIMESTAMP('2026-08-14 16:30:00')*1000),
 ('B20260801003','demo',     '演示客户','13800000000','pkg500','500M 融合套餐', 99,'刘伟','com_ns01','科技园小区','ADDON',  'DONE',    UNIX_TIMESTAMP('2026-08-25 14:02:00')*1000),
 -- 2026-09
 ('B20260914001','demo',     '演示客户','13800000000','pkg500', '500M 融合套餐', 99,'刘伟','com_ns01','科技园小区','NEW_INSTALL','PAID',      UNIX_TIMESTAMP('2026-09-14 09:12:00')*1000),
 ('B20260914002','C20260002','林小雨','13800000002','pkg1000','1000M 融合套餐',159,'赵敏','com_ns02','深大新村','NEW_INSTALL','INSTALLING', UNIX_TIMESTAMP('2026-09-14 10:05:00')*1000),
 ('B20260914003','C20260003','何大军','13800000003','pkg300', '300M 单宽带',    69,'王芳','com_ft01','香蜜湖小区','MOVE',    'PENDING',   UNIX_TIMESTAMP('2026-09-14 11:20:00')*1000);

-- ---------------------------------------------------------------------------
-- 投诉与评价
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO review (id, order_id, customer_name, worker_name, score, tags, type, content, status, created_time) VALUES
 ('RV20260912001','WO2026091201','陈先生','张伟',5,'准时,专业,速度快','REVIEW','师傅上门很准时，组网设计专业。','VISITED', UNIX_TIMESTAMP('2026-09-12 18:20:00')*1000),
 ('RV20260911001','WO2026091101','王女士','李强',3,'迟到',          'COMPLAINT','预约下午上门，实际晚上才到，影响了当天安排。','PROCESSING', UNIX_TIMESTAMP('2026-09-11 20:05:00')*1000),
 ('RV20260910001','WO2026091001','赵女士','王芳',4,'专业',          'REVIEW','布线整齐，测速达标。','CLOSED', UNIX_TIMESTAMP('2026-09-10 17:10:00')*1000);

-- ---------------------------------------------------------------------------
-- 部门（按区域划分：华南大区 > 深圳 / 广州分公司；华东大区 > 上海分公司）
-- 树形：parent_id 为空为区域根；sys_user / community / 订单 经 dept_id 归属，
-- 实现「运营人员只能看本部门及下级部门数据」的行级隔离。
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_department (id, parent_id, name, region, sort_order, status, created_time) VALUES
 ('D1', NULL, '华南大区',   '华南', 1, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D2', 'D1', '深圳分公司', '华南', 2, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D3', 'D1', '广州分公司', '华南', 3, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D4', NULL, '华东大区',   '华东', 4, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000),
 ('D5', 'D4', '上海分公司', '华东', 5, 'ENABLED', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000);

-- 已存在小区归属部门（深圳片区 -> 深圳分公司）
UPDATE community SET dept_id = 'D2' WHERE region IN ('南山','福田','宝安') AND (dept_id IS NULL OR dept_id = '');

-- 上海演示小区（归属 上海分公司，用于验证跨区数据隔离）
INSERT IGNORE INTO community (id, name, region, street, latitude, longitude, installable, port_total, port_used, dept_id) VALUES
 ('com_sh01','上海康桥花园','浦东','康桥路',31.1500,121.5500,1,200,120,'D5'),
 ('com_sh02','上海张江高科','浦东','张江路',31.2000,121.6000,1,180, 90,'D5');

-- 上海演示订单（归属 上海分公司）
INSERT IGNORE INTO biz_order (id, customer_id, customer_name, phone, package_id, package_name, amount, sales_name, community_id, community_name, order_type, status, created_time) VALUES
 ('B20260914004','C20260004','上海客户甲','13700000004','pkg1000','1000M 融合套餐',159,'上海运营','com_sh01','上海康桥花园','NEW_INSTALL','PAID', UNIX_TIMESTAMP('2026-09-14 09:30:00')*1000),
 ('B20260914005','C20260004','上海客户甲','13700000004','pkg500', '500M 融合套餐', 99,'上海运营','com_sh02','上海张江高科','RENEW',     'DONE', UNIX_TIMESTAMP('2026-09-10 14:00:00')*1000);

-- 上海演示工单
INSERT IGNORE INTO work_order (id, community_id, address, time_slot, customer_name, package_desc, status, adjacent_route, dept_id) VALUES
 ('WO2026091401','com_sh01','康桥路1号 2栋501','2026-09-15#AM','上海客户甲','1000M 融合套餐','PENDING',NULL,'D5');

-- 订单 / 工单 dept_id 经 community 推导（幂等：仅补齐未归属的）
UPDATE biz_order b JOIN community c ON c.id = b.community_id SET b.dept_id = c.dept_id WHERE b.dept_id IS NULL OR b.dept_id = '';
-- 订单片区：经 community 推导（消除销售页「负责片区」空值，幂等）
UPDATE biz_order b JOIN community c ON c.id = b.community_id SET b.region = c.region WHERE b.region IS NULL OR b.region = '';
UPDATE work_order w JOIN biz_order b ON b.id = w.biz_order_id SET w.dept_id = b.dept_id WHERE w.dept_id IS NULL OR w.dept_id = '';
-- 兜底：未关联 biz_order 的工单，直接按所属小区归属部门
UPDATE work_order w JOIN community c ON c.id = w.community_id SET w.dept_id = c.dept_id WHERE w.dept_id IS NULL OR w.dept_id = '';

-- ---------------------------------------------------------------------------
-- 退款 / 发票演示数据（对账口径：REFUND 计入退款，CANCELLED 计入取消）
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO biz_order (id, customer_id, customer_name, phone, package_id, package_name, amount, sales_name, community_id, community_name, order_type, status, created_time) VALUES
 ('B20260901001','demo',     '演示客户','13800000000','pkg500', '500M 融合套餐',  99,'刘伟','com_ns01','科技园小区','NEW_INSTALL','REFUND', UNIX_TIMESTAMP('2026-09-01 09:00:00')*1000);

INSERT IGNORE INTO order_refund (id, order_id, order_no, customer_id, customer_name, amount, reason, channel, status, refund_no, operator, created_time, handled_time) VALUES
 ('RF20260901001','B20260901001','B20260901001','demo','演示客户',99,'客户搬家，申请退款','WECHAT_MOCK','REFUNDED','RN20260902001','财务', UNIX_TIMESTAMP('2026-09-02 10:00:00')*1000, UNIX_TIMESTAMP('2026-09-02 10:30:00')*1000);

INSERT IGNORE INTO invoice_apply (id, order_id, order_no, customer_name, title, tax_no, amount, status, invoice_no, pdf_url, operator, created_time, opened_time) VALUES
 ('IN20260901001','B20260701001','B20260701001','林小雨','林小雨','',159,'OPENED','INV20260901001','/assets/invoice/INV20260901001.pdf','财务', UNIX_TIMESTAMP('2026-09-05 14:00:00')*1000, UNIX_TIMESTAMP('2026-09-05 14:20:00')*1000),
 ('IN20260901002','B20260701002','B20260701002','何大军','何大军','', 69,'PENDING',NULL,NULL,NULL, UNIX_TIMESTAMP('2026-09-06 11:00:00')*1000, NULL);

-- ===========================================================================
-- RBAC 种子：角色 / 菜单权限 / 用户 / 授权
-- 说明：用户密码留空，由后端 RbacInitializer 首次启动时写入 BCrypt 哈希
--       （默认初始密码见后端 README：admin123 / liuwei123 / ...），
--       避免在 SQL 中硬编码哈希，也保证重复启动不会覆盖已改密的数据。
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
