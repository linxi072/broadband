package com.broadband.system.model;

import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 数据字典数据项（sys_dict_data）。
 * 关联某个 dict_type，按 dict_sort 升序展示。
 */
public class SysDictData {
    @TableId("id")
    public String id;
    public String dictType;
    public String dictLabel;
    public String dictValue;
    public int dictSort;
    public String status = "ENABLED";   // ENABLED / DISABLED
    public String remark;
    public long createTime;

    public SysDictData() {}
}
