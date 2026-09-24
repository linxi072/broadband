package com.broadband.product;

import com.broadband.product.engine.PackageUpgradeEngine;
import com.broadband.product.model.PackageUpgradePreview;

/** 套餐升级引擎验证（无 Spring 依赖，直接 javac + java 运行） */
public class PackageUpgradeDemo {
    public static void main(String[] args) {
        System.out.println("=== 套餐升级补差引擎验证 ===");
        // 场景1：500M(99) → 1000M(+40) + FTTR(+30)，合约剩 540 天（18 月）
        PackageUpgradePreview p1 = PackageUpgradeEngine.compute(99, 40, 30, 540);
        System.out.println("场景1 升 1000M+FTTR: " + p1 + "  (一次性补差应=70*18=1260)");
        // 场景2：500M(99) → 2000M(+90)，合约剩 30 天
        PackageUpgradePreview p2 = PackageUpgradeEngine.compute(99, 90, 0, 30);
        System.out.println("场景2 升 2000M: " + p2 + "  (一次性补差应=90*1=90)");
        // 场景3：仅加购增值服务，带宽不变，无合约剩余
        PackageUpgradePreview p3 = PackageUpgradeEngine.compute(99, 0, 15, 0);
        System.out.println("场景3 仅加购全屋WiFi: " + p3 + "  (无合约则无一次性补差)");
        System.out.println("=== OK ===");
    }
}
