package com.broadband.product;

import com.broadband.product.engine.TrafficQueryEngine;
import com.broadband.product.model.TrafficUsageVO;

/** 流量用量引擎验证（无 Spring 依赖，直接 javac + java 运行） */
public class TrafficDemo {
    public static void main(String[] args) {
        System.out.println("=== 流量用量引擎验证 ===");
        TrafficUsageVO vo = TrafficQueryEngine.compute("C10086");
        System.out.println("套餐: " + vo.pkgName);
        System.out.println("手机流量: " + vo.mobileUsed + vo.unit + " / " + vo.mobileTotal + vo.unit
                + " (剩余 " + vo.remaining + vo.unit + ", 已用 " + vo.pct + "%)");
        System.out.println("宽带时长: " + vo.broadbandHours + "h, 峰值 " + vo.broadbandPeak);
        System.out.println("7 日趋势(G): " + vo.trend);
        System.out.println("阈值提醒: " + (vo.warn ? "剩余不足 5G，建议加购流量包" : "正常"));
        System.out.println("=== OK ===");
    }
}
