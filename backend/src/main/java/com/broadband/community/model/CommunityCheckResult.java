package com.broadband.community.model;

import java.util.List;

/**
 * 小区可安装性校验结果（GET /api/community/check 返回体）。
 */
public class CommunityCheckResult {
    public String name;             // 小区名称
    public String region;           // 所属片区
    public boolean installable;     // 是否在覆盖库且可安装
    public int portRemaining;       // 端口余量
    public String portStatus;       // AVAILABLE / TIGHT / UNAVAILABLE
    public String message;          // 给客户看的结论文案
    public boolean canProceed;      // 是否允许进入下单（false 则前端拦截）
    public String suggestion;       // 操作建议（登记需求 / 尽早下单 / 立即办理）
    public List<PackageRef> packages; // 可办套餐（仅可安装时返回）

    /** 可办套餐引用 */
    public static class PackageRef {
        public String id;
        public String name;
        public PackageRef(String id, String name) { this.id = id; this.name = name; }
    }

    public CommunityCheckResult() {}
}
