package com.broadband.system.service;

import com.broadband.system.mapper.SysDepartmentMapper;
import com.broadband.system.model.SysDepartment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门（按区域划分）服务：树形聚合、数据权限可见部门推导、基础 CRUD。
 *
 * <p>数据权限核心：{@link #visibleDeptIds(String)} 返回「本部门 + 所有下级部门」的 ID 集合；
 * 返回 {@code null} 表示「不限定部门，可见全部」（如 admin 未归属部门）。订单 / 工单列表据此做行级隔离。</p>
 */
@Service
public class SysDepartmentService {

    @Autowired private SysDepartmentMapper departmentMapper;

    /** 全部部门（不区分状态，保证树完整、数据权限推导不漏）。 */
    public List<SysDepartment> listAll() {
        return departmentMapper.selectList(null);
    }

    /** 平铺列表 -> 树（parentId 为空或父节点不存在时作为根）。 */
    public List<SysDepartment> tree() {
        List<SysDepartment> all = listAll();
        Map<String, SysDepartment> index = new HashMap<>();
        for (SysDepartment d : all) index.put(d.id, d);

        List<SysDepartment> roots = new ArrayList<>();
        for (SysDepartment d : all) {
            if (d.parentId == null || d.parentId.isEmpty() || !index.containsKey(d.parentId)) {
                roots.add(d);
            } else {
                index.get(d.parentId).children.add(d);
            }
        }
        return roots;
    }

    /** 部门名称（不存在返回空串）。 */
    public String deptName(String deptId) {
        if (deptId == null || deptId.isEmpty()) return "";
        for (SysDepartment d : listAll()) {
            if (deptId.equals(d.id)) return d.name;
        }
        return "";
    }

    /**
     * 计算某部门「可见部门集合」= 本部门 + 全部下级。
     *
     * @return 部门 ID 列表；若 {@code deptId} 为空（管理员不限定部门）返回 {@code null}，调用方据此不做过滤。
     */
    public List<String> visibleDeptIds(String deptId) {
        if (deptId == null || deptId.isEmpty()) return null;

        // 建 parent -> children 邻接表
        Map<String, List<String>> childrenMap = new LinkedHashMap<>();
        for (SysDepartment d : listAll()) {
            if (d.parentId != null && !d.parentId.isEmpty()) {
                childrenMap.computeIfAbsent(d.parentId, k -> new ArrayList<>()).add(d.id);
            }
        }

        List<String> result = new ArrayList<>();
        collect(deptId, childrenMap, result);
        return result;
    }

    private void collect(String node, Map<String, List<String>> childrenMap, List<String> out) {
        out.add(node);
        for (String child : childrenMap.getOrDefault(node, List.of())) {
            collect(child, childrenMap, out);
        }
    }

    /** 是否存在子部门（用于删除校验，禁止直接删带子的节点）。 */
    public boolean hasChildren(String deptId) {
        for (SysDepartment d : listAll()) {
            if (deptId.equals(d.parentId)) return true;
        }
        return false;
    }
}
