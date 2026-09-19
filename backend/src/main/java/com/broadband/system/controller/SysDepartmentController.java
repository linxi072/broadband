package com.broadband.system.controller;

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
 *
 * <p>本类只做权限校验与参数绑定，业务逻辑见 {@link SysDepartmentService}。</p>
 */
@RestController
@RequestMapping("/api/system/departments")
@PreAuthorize("hasAuthority('system:dept')")
public class SysDepartmentController {

    @Autowired private SysDepartmentService departmentService;

    /** 树形部门列表（用于侧边栏/下拉树渲染）。 */
    @GetMapping("/tree")
    public List<SysDepartment> tree() {
        return departmentService.tree();
    }

    /** 平铺部门列表（含停用，便于管理页展示）。 */
    @GetMapping
    public List<SysDepartment> list() {
        return departmentService.listAll();
    }

    @PostMapping
    public SysDepartment create(@RequestBody Map<String, Object> req) {
        return departmentService.create(req);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return departmentService.update(id, req);
    }

    /** 删除部门：禁止删除含子部门的节点（需先清理下级）。 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return departmentService.delete(id);
    }
}
