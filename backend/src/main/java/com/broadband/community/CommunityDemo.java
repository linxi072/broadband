package com.broadband.community;

import com.broadband.community.engine.CommunityChecker;
import com.broadband.community.model.CommunityCheckResult;
import com.broadband.install.model.Community;
import com.broadband.product.model.PackageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 小区可安装性判定演示（纯 Java，无 Spring 依赖）。
 * 覆盖：正常可装 / 端口紧张 / 未覆盖 / 未收录 四种场景。
 */
public class CommunityDemo {

    public static void main(String[] args) {
        List<PackageInfo> onShelf = new ArrayList<>();
        PackageInfo p1 = new PackageInfo(); p1.id = "pkg-500"; p1.name = "500M 融合套餐";
        PackageInfo p2 = new PackageInfo(); p2.id = "pkg-1000"; p2.name = "1000M 千兆融合";
        onShelf.add(p1); onShelf.add(p2);

        Community c1 = new Community("C1", "南山科技园", "南山", "科技园路", 22.53, 113.95);
        c1.portTotal = 100; c1.portUsed = 5;            // 余量 95 -> AVAILABLE

        Community c2 = new Community("C2", "保利花园", "南山", "海德三道", 22.51, 113.93);
        c2.portTotal = 20; c2.portUsed = 19;            // 余量 1 -> TIGHT

        Community c3 = new Community("C3", "未覆盖村", "坪山", "xxx路", 22.69, 114.33);
        c3.installable = false;                          // UNAVAILABLE

        demo("南山科技园", CommunityChecker.evaluate(c1, onShelf));
        demo("保利花园", CommunityChecker.evaluate(c2, onShelf));
        demo("未覆盖村", CommunityChecker.evaluate(c3, onShelf));
        demo("火星小区（未收录）", CommunityChecker.evaluate(null, onShelf));
    }

    private static void demo(String title, CommunityCheckResult r) {
        System.out.println("=== " + title + " ===");
        System.out.println("  结论：" + r.message);
        System.out.println("  端口状态：" + r.portStatus + " | 余量：" + r.portRemaining + " | 可下单：" + r.canProceed);
        System.out.println("  建议：" + r.suggestion + " | 可办套餐：" + (r.packages == null ? 0 : r.packages.size()) + " 个");
    }
}
