package com.broadband.system.model;

/** 操作日志（对应 DB 表 sys_oper_log）。 */
public class SysOperLog {
    public Long id;
    public String username;
    public String name;
    public String action;
    public String target;
    public String method;
    public String ip;
    public String result;        // 成功 / 拒绝(403) / 未认证(401) / 失败
    public long costMs;
    public long createdTime;

    public SysOperLog() {}

    public SysOperLog(String username, String name, String action, String target,
                      String method, String ip, String result, long costMs) {
        this.username = username;
        this.name = name;
        this.action = action;
        this.target = target;
        this.method = method;
        this.ip = ip;
        this.result = result;
        this.costMs = costMs;
        this.createdTime = System.currentTimeMillis();
    }
}
