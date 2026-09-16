package com.broadband.system.model;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门（按区域划分，对应 DB 表 sys_department）。
 * 树形：parentId 为空表示区域根节点（如「华南大区」）。
 * children 字段为树形聚合用，不落库。
 */
public class SysDepartment {
    public String id;
    public String parentId;
    public String name;
    public String region;
    public int sortOrder;
    public String status = "ENABLED";   // ENABLED / DISABLED
    public long createdTime;
    @TableField(exist = false)
    public List<SysDepartment> children = new ArrayList<>();

    public SysDepartment() {}
}
