package com.broadband.system.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * v1.14 / v1.15 业务数据与 RBAC 初始化（幂等）。
 *
 * <p>负责：
 * ① 注册「运营留存」菜单分组（M160）及其 4 个子菜单（积分成长 / 优惠活动 / 在线客服 / 账户账单），
 *    权限码与 web-admin 的 navConfig 一一对应（points:view / promotion:view / support:view / account:view）；
 * ② 注册 v1.15 新增菜单：数据智能（M150，intelligence:view）、支付管理（M180，payment:view）；
 * ③ 注入基础种子数据：积分任务、积分商城商品、优惠活动、客服 FAQ，以及 v1.15 智能营销规则（intel_campaign）。</p>
 *
 * <p>全部以 INSERT IGNORE / 存在性判定实现幂等，可重复执行；走 ApplicationRunner 而非 data.sql，
 * 聚焦于本次迭代新增内容，不打扰既有骨架种子。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备网络/本地仓库的机器上
 * `mvn -o compile` 或 `mvn package` 验证编译通过后再合并。</p>
 */
@Component
public class BizDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BizDataInitializer.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        seedRbac();
        seedPointsTasks();
        seedPointsMall();
        seedPromotions();
        seedFaqs();
        seedIntelligenceCampaigns();
        log.info("v1.14 运营留存 + v1.15 数据智能/支付管理 + 数据分析深化：菜单权限与基础数据初始化完成。");
    }

    // ---------------------------------------------------------------- RBAC 菜单与授权

    private void seedRbac() {
        // 顶级分组「运营留存」(M160, DIR)。子菜单挂其下，与 web-admin navConfig 镜像。
        // v1.15 新增：数据智能(M150, intelligence:view)、支付管理(M180, payment:view)。
        // 注：此前迭代计划文档将 M150 标注为 intelligence 菜单、M180 标注为支付菜单，
        // 现 v1.15 已真正落地对应模块，故 M150/M180 由文档占位转为真实菜单（与后端接口权限一致）。
        jdbc.update("""
                INSERT IGNORE INTO sys_menu (id, parent_id, name, path, perm, type, sort_order)
                VALUES ('M160', NULL, '运营留存', '', 'ops:group', 'DIR', 13),
                       ('M161', 'M160', '积分成长', '/points',    'points:view',    'MENU', 1),
                       ('M162', 'M160', '优惠活动', '/promotion', 'promotion:view', 'MENU', 2),
                       ('M163', 'M160', '在线客服', '/support',   'support:view',   'MENU', 3),
                       ('M164', 'M160', '账户账单', '/account',   'account:view',   'MENU', 4),
                       ('M150', NULL, '数据智能', '/intelligence',      'intelligence:view', 'DIR', 14),
                       ('M151', 'M150', '客户分群与智能营销', '/intelligence', 'intelligence:view', 'MENU', 1),
                       ('M180', NULL, '支付管理', '/admin/pay/transactions', 'payment:view', 'DIR', 15),
                       ('M181', 'M180', '支付流水', '/admin/pay/transactions', 'payment:view', 'MENU', 1),
                       ('M182', 'M180', '退款处理', '/admin/pay/refund',      'payment:view', 'MENU', 2),
                       ('M190', NULL, '数据分析深化', '/analytics/customer360', 'analytics:view', 'DIR', 16),
                       ('M191', 'M190', '客户360', '/analytics/customer360', 'analytics:view', 'MENU', 1),
                       ('M192', 'M190', '营销漏斗', '/analytics/funnel',      'analytics:view', 'MENU', 2),
                       ('M193', 'M190', 'SLA超时与赔付', '/analytics/sla',    'analytics:view', 'MENU', 3)
                """);

        // 授权给 ADMIN / OPERATOR（与 DictConfigInitializer 同源）
        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT id FROM sys_role WHERE code IN ('ADMIN','OPERATOR')");
        for (Map<String, Object> r : roles) {
            String roleId = String.valueOf(r.get("id"));
            for (String menuId : new String[]{"M160", "M161", "M162", "M163", "M164",
                    "M150", "M151", "M180", "M181", "M182",
                    "M190", "M191", "M192", "M193"}) {
                Integer cnt = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ? AND menu_id = ?",
                        Integer.class, roleId, menuId);
                if (cnt != null && cnt == 0) {
                    jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
                }
            }
        }
    }

    // ---------------------------------------------------------------- 智能营销规则

    private void seedIntelligenceCampaigns() {
        // v1.15 数据智能：智能营销自动化规则种子（客户分群 -> 渠道/内容/触发方式）。
        jdbc.update("""
                INSERT IGNORE INTO intel_campaign (id, name, segment, channel, content, trigger_type, status, reach_count, created_time)
                VALUES
                  ('IC_NEW',    '新客首单关怀',    'NEW',         'SMS',   '欢迎办理宽带，首月体验专属提速包，详询客服。', 'AUTO', 'ENABLED', 0, 0),
                  ('IC_RISK',   '流失预警挽回',    'CHURN_RISK',  'PUSH',  '好久不见～专属续费优惠限时领取，回TA续享高速宽带。', 'AUTO', 'ENABLED', 0, 0),
                  ('IC_COMPL',  '投诉关怀回访',    'COMPLAINT',   'SMS',   '非常抱歉给您带来不便，专属客服将尽快回访处理。', 'MANUAL', 'ENABLED', 0, 0),
                  ('IC_VIP',    '高价值客户权益',  'HIGH_VALUE',  'COUPON', '尊敬的VIP客户，赠送5G提速周卡，感恩一路相伴。', 'MANUAL', 'ENABLED', 0, 0),
                  ('IC_ATRISK', '活跃预警激活',    'AT_RISK',     'PUSH',  '您有专属提速券待领取，立即体验千兆极速。', 'AUTO', 'ENABLED', 0, 0)
                """);
    }

    // ---------------------------------------------------------------- 积分任务

    private void seedPointsTasks() {
        jdbc.update("""
                INSERT IGNORE INTO points_task (id, task_key, name, points, description, sort_order, status)
                VALUES
                  ('PT_SIGN',        'signin',       '每日签到',  5,   '连续签到赢积分',        1, 'ENABLED'),
                  ('PT_PROFILE',     'profile',      '完善资料',  50,  '补全实名与安装地址',    2, 'ENABLED'),
                  ('PT_FIRST_REVIEW','first_review', '首单评价',  30,  '完成一次安装评价',      3, 'ENABLED'),
                  ('PT_INVITE',      'invite',       '邀请好友',  100, '成功邀请 1 位好友办理', 4, 'ENABLED')
                """);
    }

    // ---------------------------------------------------------------- 积分商城商品

    private void seedPointsMall() {
        jdbc.update("""
                INSERT IGNORE INTO points_mall_item (id, name, cost, stock, coupon_type, coupon_value, image, status)
                VALUES
                  ('PM1', '5G 提速包（7 天）',    200, 99, 'SPEED_UP', '提速至 500M',     NULL, 'ON_SHELF'),
                  ('PM2', '腾讯视频月卡',         500, 50, 'VOUCHER',  '腾讯视频月度会员', NULL, 'ON_SHELF'),
                  ('PM3', '路由器抵扣券 ¥30',     800, 20, 'VOUCHER',  '¥30 路由器抵扣',   NULL, 'ON_SHELF')
                """);
    }

    // ---------------------------------------------------------------- 优惠活动

    private void seedPromotions() {
        jdbc.update("""
                INSERT IGNORE INTO promotion (id, title, subtitle, cover, type, target, start_date, end_date, rule_json, status)
                VALUES
                  ('P1', '千兆融合限时直降', '月费直降 30 元，连续 12 期', NULL, 'LIMITED', 'ALL', '2026-09-01', '2026-09-30', '{"cut":30,"months":12}', 'ONLINE'),
                  ('P2', '老用户续约送时长', '合约续约赠送 3 个月',      NULL, 'ANNUAL',  'ALL', '2026-09-10', '2026-10-10', '{"giftMonths":3}',        'ONLINE'),
                  ('P3', '宽带+电视全家桶',  '办宽带送 IPTV',           NULL, 'COMBO',   'ALL', '2026-09-15', '2026-12-15', '{"gift":"iptv"}',        'ONLINE')
                """);
    }

    // ---------------------------------------------------------------- 客服 FAQ

    private void seedFaqs() {
        // 分类与 web-admin 在线客服页筛选项（安装/故障/账单/套餐）保持一致
        jdbc.update("""
                INSERT IGNORE INTO support_faq (id, category, question, answer, sort_order)
                VALUES
                  ('F1', '安装', '新装宽带多久能上门？',     '城区通常 24 小时内预约，48 小时内完成安装。', 1),
                  ('F2', '故障', '宽带突然断网怎么办？',     '请先重启光猫与路由器；仍异常可提交报修工单，师傅将主动联系。', 2),
                  ('F3', '账单', '如何开具电子发票？',       '在「我的-账单」选择订单申请发票，财务审核后推送电子票。', 3),
                  ('F4', '套餐', '合约期内能升级带宽吗？',   '支持补差升级，按剩余合约月数折算一次性补差费用。', 4)
                """);
    }
}
