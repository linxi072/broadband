package com.broadband.system.spring;

import com.broadband.common.CsvUtil;
import com.broadband.common.Ids;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysRoleMapper;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.SysMenu;
import com.broadband.system.model.SysRole;
import com.broadband.system.model.SysUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理数据导入 / 导出（CSV）。
 *
 * <p>导出：UTF-8 BOM + CSV，浏览器可直接用 Excel 打开。
 * 导入：上传 CSV（multipart），批量创建；已存在的账号 / 角色标识自动跳过，返回汇总。
 * 选 CSV 而非 Excel 是为了零三方依赖、离线可构建（沙箱 Maven 离线、未缓存 POI）。</p>
 */
@RestController
@RequestMapping("/api/system")
public class SystemDataController {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OperLogService operLog;

    // ==================================================================== 用户

    @GetMapping("/users/export")
    @PreAuthorize("hasAuthority('system:user')")
    public void exportUsers(HttpServletResponse resp) throws Exception {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"账号", "姓名", "部门", "角色编码", "状态", "初始密码"});
        for (SysUser u : userMapper.selectList(null)) {
            List<String> codes = new ArrayList<>();
            userMapper.selectRoles(u.id).forEach(r -> codes.add(r.code));
            rows.add(new String[]{
                    u.username, u.name, u.deptId == null ? "" : u.deptId,
                    String.join(",", codes), u.status, ""
            });
        }
        writeCsv(resp, "用户数据.csv", rows);
        log("导出用户数据", "/api/system/users/export", "GET");
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:user')")
    public Map<String, Object> importUsers(@RequestParam("file") MultipartFile file) throws Exception {
        List<String[]> rows = CsvUtil.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        if (!rows.isEmpty()) rows.remove(0); // 去表头

        Map<String, String> roleCodeToId = new LinkedHashMap<>();
        roleMapper.selectList(null).forEach(r -> roleCodeToId.put(r.code, r.id));

        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 2 || (row[0].isBlank() && row[1].isBlank())) continue;
            String username = row[0].trim();
            String name = row[1].trim();
            if (username.isEmpty() || name.isEmpty()) { errors.add("账号/姓名缺失：" + String.join("|", row)); continue; }
            if (userMapper.selectByUsername(username) != null) { skipped++; continue; }

            SysUser u = new SysUser();
            u.id = Ids.next();
            u.username = username;
            u.name = name;
            u.deptId = row.length > 2 ? row[2].trim() : null;
            u.status = "ENABLED";
            u.createdTime = System.currentTimeMillis();
            String raw = row.length > 5 && !row[5].isBlank() ? row[5].trim() : "123456";
            u.password = passwordEncoder.encode(raw);
            userMapper.insert(u);

            if (row.length > 3 && !row[3].isBlank()) {
                for (String code : row[3].split("[,|，]")) {
                    String cid = roleCodeToId.get(code.trim());
                    if (cid != null) userMapper.insertUserRole(u.id, cid);
                }
            }
            created++;
        }
        log("导入用户数据（新增" + created + "/跳过" + skipped + "）", "/api/system/users/import", "POST");
        return Map.of("total", rows.size(), "created", created, "skipped", skipped, "errors", errors);
    }

    // ==================================================================== 角色

    @GetMapping("/roles/export")
    @PreAuthorize("hasAuthority('system:role')")
    public void exportRoles(HttpServletResponse resp) throws Exception {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"角色标识", "角色名称", "说明"});
        for (SysRole r : roleMapper.selectList(null)) {
            rows.add(new String[]{r.code, r.name, r.remark == null ? "" : r.remark});
        }
        writeCsv(resp, "角色数据.csv", rows);
        log("导出角色数据", "/api/system/roles/export", "GET");
    }

    @PostMapping(value = "/roles/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:role')")
    public Map<String, Object> importRoles(@RequestParam("file") MultipartFile file) throws Exception {
        List<String[]> rows = CsvUtil.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        if (!rows.isEmpty()) rows.remove(0);

        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 2 || (row[0].isBlank() && row[1].isBlank())) continue;
            String code = row[0].trim();
            String name = row[1].trim();
            if (code.isEmpty() || name.isEmpty()) { errors.add("角色标识/名称缺失：" + String.join("|", row)); continue; }
            if (roleMapper.selectByCode(code) != null) { skipped++; continue; }
            SysRole r = new SysRole();
            r.id = Ids.next();
            r.code = code;
            r.name = name;
            r.remark = row.length > 2 ? row[2].trim() : null;
            roleMapper.insert(r);
            created++;
        }
        log("导入角色数据（新增" + created + "/跳过" + skipped + "）", "/api/system/roles/import", "POST");
        return Map.of("total", rows.size(), "created", created, "skipped", skipped, "errors", errors);
    }

    // ==================================================================== 菜单

    @GetMapping("/menus/export")
    @PreAuthorize("hasAuthority('system:menu')")
    public void exportMenus(HttpServletResponse resp) throws Exception {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"名称", "类型", "路径", "权限码", "父级路径", "排序"});
        Map<String, String> idToPath = new LinkedHashMap<>();
        for (SysMenu m : menuMapper.selectAll()) {
            idToPath.put(m.id, m.path == null ? "" : m.path);
        }
        for (SysMenu m : menuMapper.selectAll()) {
            rows.add(new String[]{
                    m.name, m.type, m.path == null ? "" : m.path, m.perm == null ? "" : m.perm,
                    m.parentId == null ? "" : (idToPath.getOrDefault(m.parentId, "")),
                    String.valueOf(m.sortOrder)
            });
        }
        writeCsv(resp, "菜单数据.csv", rows);
        log("导出菜单数据", "/api/system/menus/export", "GET");
    }

    // ==================================================================== 工具

    private void writeCsv(HttpServletResponse resp, String filename, List<String[]> rows) throws Exception {
        String csv = "﻿" + CsvUtil.toCsv(rows); // BOM 让 Excel 正确识别 UTF-8
        resp.setContentType("text/csv;charset=utf-8");
        String enc = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + enc);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(csv);
        resp.getWriter().flush();
    }

    private void log(String action, String target, String method) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
