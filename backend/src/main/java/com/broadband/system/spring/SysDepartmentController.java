package com.broadband.system.spring;

import com.broadband.common.Ids;
import com.broadband.system.mapper.SysDepartmentMapper;
import com.broadband.system.model.SysDepartment;
import com.broadband.system.service.SysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 部门管理（按区域划分）：树形列表 / 新增 / 编辑 / 删除，数据权限的基础。
 * 权限码 {@code system:dept} 与前端 navConfig、@PreAuthorize 三处同源。
 */
@RestController
@RequestMapping("/api/system/departments")
@PreAuthorize("hasAuthority('system:dept')")
public class SysDepartmentController {

    @Autowired private SysDepartmentMapper departmentMapper;
    @Autowired private SysDepartmentService departmentService;

    /** 树形部门列表（用于侧边栏/下拉树渲染）。 */
    @GetMapping("/tree")
    public List<SysDepartment> tree() {
        return departmentService.tree();
    }

    /** 平铺部门列表（含停用，便于管理页展示）。 */
    @GetMapping
    public List<SysDepartment> list() {
        return departmentMapper.selectList(null);
    }

    @PostMapping
    public SysDepartment create(@RequestBody Map<String, Object> req) {
        SysDepartment d = new SysDepartment();
        d.id = Ids.next();
        d.parentId = str(req.get("parentId"));
        d.name = require(str(req.get("name")), "部门名称不能为空");
        d.region = str(req.get("region"));
        d.sortOrder = asInt(req.get("sortOrder"), 0);
        d.status = "ENABLED";
        d.createdTime = System.currentTimeMillis();
        departmentMapper.insert(d);
        log("新增部门 " + d.name);
        return d;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        SysDepartment exist = departmentMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("部门不存在：" + id);
        // 不允许把父节点设为自己或自己的子孙（避免成环）
        String newParent = str(req.get("parentId"));
        if (newParent != null && !newParent.isEmpty() && !newParent.equals(exist.parentId)) {
            if (newParent.equals(id) || departmentService.visibleDeptIds(id).contains(newParent)) {
                throw new IllegalArgumentException("不能将部门挂到自身或其下级之下");
            }
        }
        SysDepartment patch = new SysDepartment();
        patch.id = id;
        patch.parentId = newParent;
        patch.name = str(req.get("name")) == null ? exist.name : str(req.get("name"));
        patch.region = str(req.get("region")) == null ? exist.region : str(req.get("region"));
        patch.sortOrder = req.get("sortOrder") == null ? exist.sortOrder : asInt(req.get("sortOrder"), exist.sortOrder);
        patch.status = str(req.get("status")) == null ? exist.status : str(req.get("status"));
        departmentMapper.updateById(patch);
        log("编辑部门 " + id);
        return Map.of("ok", true);
    }

    /** 删除部门：禁止删除含子部门的节点（需先清理下级）。 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        if (departmentService.hasChildren(id)) {
            throw new IllegalArgumentException("该部门下存在子部门，请先删除子部门");
        }
        departmentMapper.deleteById(id);
        log("删除部门 " + id);
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ 辅助

    private void log(String action) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username,
                me == null ? null : me.user.name,
                action, "/api/system/departments", "WRITE", "-", "成功", 0);
    }

    @Autowired private OperLogService operLog;

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String require(String v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }

    private static int asInt(Object v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}
