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
 * 数据字典 / 参数配置 初始化（幂等）。
 *
 * <p>负责：① 注册「数据字典 system:dict」「参数配置 system:config」两个菜单与权限码，
 * 并授权给 ADMIN / OPERATOR 角色（与 sys_menu / sys_role_menu 同源，使 @PreAuthorize 生效）；
 * ② 注入默认字典（故障类型、工单类型）与系统参数（客服电话、报修承诺时长、派单默认容量等）。
 * 全部以 INSERT IGNORE / 存在性判定实现幂等，可重复执行。</p>
 *
 * <p>之所以不走 data.sql：data.sql 仅承载少量骨架种子，字典/配置属本次迭代新增，
 * 用 ApplicationRunner 注入更聚焦，且不打扰既有种子脚本。</p>
 */
@Component
public class DictConfigInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DictConfigInitializer.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        seedRbac();
        seedDict();
        seedConfig();
        log.info("数据字典/参数配置初始化完成。");
    }

    // ---------------------------------------------------------------- RBAC 菜单与授权

    private void seedRbac() {
        // 系统管理父菜单 M13（部门管理 M44 即挂在其下）。两个新菜单作为其同级子节点。
        // 注意：data.sql 已预置 M70=数据字典(system:dict)，此处用 INSERT IGNORE 兼容「全新库」；
        // 参数配置菜单取未被占用的高位 id（M140，避免与既有工单池 M71 / 派单 M72 等冲突）。
        jdbc.update("""
                INSERT IGNORE INTO sys_menu (id, parent_id, name, path, perm, type, sort_order)
                VALUES ('M70', 'M13', '数据字典', '/system/dict', 'system:dict', 'MENU', 6),
                       ('M140', 'M13', '参数配置', '/system/config', 'system:config', 'MENU', 7)
                """);

        // 授权给 ADMIN / OPERATOR
        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT id FROM sys_role WHERE code IN ('ADMIN','OPERATOR')");
        for (Map<String, Object> r : roles) {
            String roleId = String.valueOf(r.get("id"));
            for (String menuId : new String[]{"M70", "M140"}) {
                Integer cnt = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ? AND menu_id = ?",
                        Integer.class, roleId, menuId);
                if (cnt != null && cnt == 0) {
                    jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
                }
            }
        }
    }

    // ---------------------------------------------------------------- 默认字典

    private void seedDict() {
        jdbc.update("""
                INSERT IGNORE INTO sys_dict_type (dict_type, dict_name, status, remark, create_time)
                VALUES ('fault_category', '故障类型', 'ENABLED', '故障报修工单的故障分类', ?),
                       ('work_order_type', '工单类型', 'ENABLED', '安装工单业务类型', ?)
                """, now(), now());

        // 故障类型数据项
        jdbc.update("""
                INSERT IGNORE INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_sort, status, create_time)
                VALUES
                  ('D_FAULT_NET',   'fault_category', '网络中断',   'NETWORK_DOWN', 1, 'ENABLED', ?),
                  ('D_FAULT_SLOW',  'fault_category', '网速慢/卡顿', 'SLOW',         2, 'ENABLED', ?),
                  ('D_FAULT_DROP',  'fault_category', '频繁掉线',   'DROPOUT',      3, 'ENABLED', ?),
                  ('D_FAULT_NOCONN','fault_category', '无法连接',   'NO_CONNECT',   4, 'ENABLED', ?),
                  ('D_FAULT_DEV',   'fault_category', '设备故障',   'DEVICE',       5, 'ENABLED', ?),
                  ('D_FAULT_OTHER', 'fault_category', '其他',       'OTHER',        6, 'ENABLED', ?)
                """, now(), now(), now(), now(), now(), now());

        // 工单类型数据项
        jdbc.update("""
                INSERT IGNORE INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_sort, status, create_time)
                VALUES
                  ('D_WOT_INSTALL', 'work_order_type', '新装宽带', 'INSTALL',  1, 'ENABLED', ?),
                  ('D_WOT_REPAIR',  'work_order_type', '故障报修', 'REPAIR',   2, 'ENABLED', ?),
                  ('D_WOT_MOVE',    'work_order_type', '宽带移机', 'MOVE',     3, 'ENABLED', ?),
                  ('D_WOT_SPEED',   'work_order_type', '宽带提速', 'SPEED_UP', 4, 'ENABLED', ?),
                  ('D_WOT_RENEW',   'work_order_type', '续费',     'RENEW',    5, 'ENABLED', ?)
                """, now(), now(), now(), now(), now());
    }

    // ---------------------------------------------------------------- 默认参数

    private void seedConfig() {
        jdbc.update("""
                INSERT IGNORE INTO sys_config (config_key, config_name, config_value, config_type, remark, create_time)
                VALUES
                  ('repair.sla.promised.hours', '报修承诺修复小时', '24', 'INT', '故障报修 SLA 承诺上门修复时限（小时）', ?),
                  ('customer.service.phone',     '客服电话',        '10099', 'STRING', '客户报修/咨询统一客服热线', ?),
                  ('dispatch.default.adjacent.cap',     '派单默认相邻容量', '4', 'INT', '相邻小区每时段派单上限（默认）', ?),
                  ('dispatch.default.non.adjacent.cap', '派单默认非相邻容量', '2', 'INT', '非相邻小区每时段派单上限（默认）', ?),
                  ('app.notice',                 '报修提示语', '报修后师傅将在承诺时限内联系上门，请保持电话畅通', 'STRING', 'C 端报修页提示', ?)
                """, now(), now(), now(), now(), now());
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
