package com.broadband.community.engine;

import com.broadband.community.model.CommunityCheckResult;
import com.broadband.install.model.Community;
import com.broadband.product.model.PackageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 小区可安装性判定引擎（纯 Java，无 Spring 依赖，可单测）。
 * 规则：
 *  - 不在覆盖库 / 未开通        -> UNAVAILABLE（拦截下单）
 *  - 端口余量 <= TIGHT_THRESHOLD -> TIGHT（放行但强提示尽早）
 *  - 其余                        -> AVAILABLE（正常放行）
 */
public final class CommunityChecker {

    /** 端口余量 <= 该值视为「紧张」 */
    public static final int TIGHT_THRESHOLD = 2;

    private CommunityChecker() {}

    public static CommunityCheckResult evaluate(Community c, List<PackageInfo> onShelfPackages) {
        CommunityCheckResult r = new CommunityCheckResult();

        if (c == null) {
            r.installable = false;
            r.portStatus = "UNAVAILABLE";
            r.portRemaining = 0;
            r.message = "未收录该小区，暂无法确认能否安装";
            r.canProceed = false;
            r.suggestion = "可登记安装需求，覆盖后第一时间通知您";
            r.packages = new ArrayList<>();
            return r;
        }

        r.name = c.name;
        r.region = c.region;

        if (!c.installable) {
            r.installable = false;
            r.portStatus = "UNAVAILABLE";
            r.portRemaining = Math.max(0, c.portTotal - c.portUsed);
            r.message = "该小区暂未覆盖宽带资源";
            r.canProceed = false;
            r.suggestion = "可登记安装需求，覆盖后第一时间通知您";
            r.packages = new ArrayList<>();
            return r;
        }

        int remaining = Math.max(0, c.portTotal - c.portUsed);
        r.portRemaining = remaining;

        if (remaining <= 0) {
            r.installable = false;
            r.portStatus = "UNAVAILABLE";
            r.message = "端口已占满，暂时无法新装";
            r.canProceed = false;
            r.suggestion = "可登记安装需求，释放后通知您";
            r.packages = new ArrayList<>();
            return r;
        }

        if (remaining <= TIGHT_THRESHOLD) {
            r.installable = true;
            r.portStatus = "TIGHT";
            r.message = "端口紧张（余量 " + remaining + "），建议尽早下单";
            r.canProceed = true;
            r.suggestion = "端口紧张，建议尽早下单锁定资源";
        } else {
            r.installable = true;
            r.portStatus = "AVAILABLE";
            r.message = "可安装（端口余量 " + remaining + "）";
            r.canProceed = true;
            r.suggestion = "可立即办理";
        }

        r.packages = new ArrayList<>();
        if (onShelfPackages != null) {
            for (PackageInfo p : onShelfPackages) {
                r.packages.add(new CommunityCheckResult.PackageRef(p.id, p.name));
            }
        }
        return r;
    }
}
