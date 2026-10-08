package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 住宅管理（US-3.1 多住宅切换 · v1.19 新模块 · PC 后台）。
 *
 * <p>支撑「一个账号 / 家庭管理多个住宅」的场景：住宅作为<b>数据隔离维度</b>，
 * 客户通过 {@code customer.household_id} 挂载到具体住宅，台账、订单等业务数据据此隔离。</p>
 *
 * <p>本控制器负责住宅本体（住宅台账、概览、新建）的维护；住宅 ↔ 客户的挂载关系
 * 由 {@code customer.household_id} 承载（在客户台账 / 订单等列表按住宅过滤即可实现隔离）。</p>
 *
 * <p>权限 {@code household:manage}（M230/M231 菜单授权 ADMIN/OPERATOR）。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备网络/本地仓库的机器上
 * {@code mvn -o compile} 验证编译通过后再合并。</p>
 */
@RestController
@RequestMapping("/api/admin/household")
public class HouseholdController {

    @Autowired
    private JdbcTemplate jdbc;

    // ============================================================ 住宅台账

    /**
     * 住宅台账：支持关键字（住宅名 / 地址 / 户主）筛选与分页。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('household:manage')")
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (h.name LIKE ? OR h.address LIKE ? OR h.owner_name LIKE ?) ");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }

        int total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM household h" + where,
                Integer.class, args.toArray());

        String sql = """
                SELECT h.id, h.name, h.address, h.community_id AS communityId,
                       com.name AS communityName, h.owner_name AS ownerName,
                       h.owner_phone AS ownerPhone, h.status, h.created_time AS createdTime
                FROM household h
                LEFT JOIN community com ON com.id = h.community_id
                """ + where + """
                ORDER BY h.created_time DESC
                LIMIT ? OFFSET ?
                """;

        List<Object> pageArgs = new ArrayList<>(args);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        pageArgs.add(safeSize);
        pageArgs.add((safePage - 1) * safeSize);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, pageArgs.toArray());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", rows);
        resp.put("total", total);
        resp.put("page", safePage);
        resp.put("size", safeSize);
        return resp;
    }

    // ============================================================ 概览统计

    /**
     * 住宅概览：住宅总数 / 启用 / 停用；并统计各住宅下挂客户数（customer.household_id）。
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('household:manage')")
    public Map<String, Object> stats() {
        Map<String, Object> base = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS active,
                       COALESCE(SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END), 0) AS inactive
                FROM household
                """);
        Integer customerLinked = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE household_id IS NOT NULL AND household_id <> ''",
                Integer.class);
        Map<String, Object> resp = new LinkedHashMap<>(base);
        resp.put("customerLinked", customerLinked == null ? 0 : customerLinked);
        return resp;
    }

    // ============================================================ 新建住宅

    /**
     * 新建住宅：入参 {name, address, communityId, ownerName, ownerPhone}。
     * id 由服务端生成；name 必填。
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('household:manage')")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("住宅名称必填");
        }
        String id = "H" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        String address = str(body, "address");
        String communityId = str(body, "communityId");
        String ownerName = str(body, "ownerName");
        String ownerPhone = str(body, "ownerPhone");
        Long now = System.currentTimeMillis();

        jdbc.update(
                "INSERT INTO household (id, name, address, community_id, owner_name, owner_phone, status, created_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)",
                id, name, address, communityId, ownerName, ownerPhone, now);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("name", name);
        return resp;
    }

    // ============================================================ 内部辅助

    private static String str(Map<String, Object> body, String k) {
        Object v = body.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
