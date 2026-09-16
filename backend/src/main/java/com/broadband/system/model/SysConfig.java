package com.broadband.system.model;

import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 参数配置（sys_config）。
 * 系统级可在线维护的参数（客服电话 / 报修承诺时长 / 派单默认容量等）。
 * config_key 为主键，config_type 仅作展示与前端表单类型提示。
 */
public class SysConfig {
    @TableId("config_key")
    public String configKey;
    public String configName;
    public String configValue;
    public String configType = "STRING"; // STRING / INT / BOOLEAN / JSON
    public String remark;
    public long createTime;

    public SysConfig() {}
}
