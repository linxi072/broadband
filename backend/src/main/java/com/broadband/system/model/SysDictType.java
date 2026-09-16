package com.broadband.system.model;

import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 数据字典类型（sys_dict_type）。
 * 例如 fault_category（故障类型）、work_order_type（工单类型）。
 */
public class SysDictType {
    @TableId("dict_type")
    public String dictType;
    public String dictName;
    public String status = "ENABLED";   // ENABLED / DISABLED
    public String remark;
    public long createTime;

    public SysDictType() {}
}
