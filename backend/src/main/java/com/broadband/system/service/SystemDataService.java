package com.broadband.system.service;

import com.broadband.common.CsvUtil;
import com.broadband.common.Ids;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysRoleMapper;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.SysMenu;
import com.broadband.system.model.SysRole;
import com.broadband.system.model.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
 * 选 CSV 而非 Excel 是为了零三方依赖、离线可构建。</p>
 *
 * <p>本类只产出「文件名 + CSV 文本」，HTTP 响应头的写入留在 Controller（Web 层职责）。</p>
 */
@Service
public class SystemDataService {

    /** 一次 CSV 导出结果：文件名 + 内容（含 BOM）。 */
    public record CsvDownload(String filename, String content) {
    }

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OperLogService operLog;

    // ==================================================================== 用户

    public CsvDownload exportUsers() {
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
        log("导出用户数据", "/api/system/users/export", "GET");
        return csv("用户数据.csv", rows);
    }

    /** 导入用户 CSV：已存在账号跳过，可按角色编码绑定角色。 */
    public Map<String, Object> importUsers(MultipartFile file) throws IOException {
        List<String[]> rows = parse(file);
        Map<String, String> roleCodeToId = new LinkedHashMap<>();
        roleMapper.selectList(null).forEach(r -> roleCodeToId.put(r.code, r.id));

        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 2 || (row[0].isBlank() && row[1].isBlank())) continue;
            String username = row[0].trim();
            String name = row[1].trim();
            if (username.isEmpty() || name.isEmpty()) {
                errors.add("账号/姓名缺失：" + String.join("|", row));
                continue;
            }
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

    public CsvDownload exportRoles() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"角色标识", "角色名称", "说明"});
        for (SysRole r : roleMapper.selectList(null)) {
            rows.add(new String[]{r.code, r.name, r.remark == null ? "" : r.remark});
        }
        log("导出角色数据", "/api/system/roles/export", "GET");
        return csv("角色数据.csv", rows);
    }

    /** 导入角色 CSV：已存在角色标识跳过。 */
    public Map<String, Object> importRoles(MultipartFile file) throws IOException {
        List<String[]> rows = parse(file);
        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 2 || (row[0].isBlank() && row[1].isBlank())) continue;
            String code = row[0].trim();
            String name = row[1].trim();
            if (code.isEmpty() || name.isEmpty()) {
                errors.add("角色标识/名称缺失：" + String.join("|", row));
                continue;
            }
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

    public CsvDownload exportMenus() {
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
        log("导出菜单数据", "/api/system/menus/export", "GET");
        return csv("菜单数据.csv", rows);
    }

    // ==================================================================== 辅助

    /** 解析上传的 CSV（去掉表头）。 */
    private List<String[]> parse(MultipartFile file) throws IOException {
        List<String[]> rows = CsvUtil.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        if (!rows.isEmpty()) rows.remove(0);
        return rows;
    }

    /** 拼装 CSV 文本：前置 BOM 让 Excel 正确识别 UTF-8。 */
    private CsvDownload csv(String filename, List<String[]> rows) {
        return new CsvDownload(filename, "\uFEFF" + CsvUtil.toCsv(rows));
    }

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
