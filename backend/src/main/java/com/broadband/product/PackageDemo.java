package com.broadband.product;

import com.broadband.product.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 套餐详情组装演示（纯 Java，无 Spring 依赖）。
 * 模拟 PackageServiceImpl.getDetail 的 DB 聚合结果，用于单测数据结构与价格计算。
 */
public class PackageDemo {

    public static void main(String[] args) {
        // 1) 主表
        PackageInfo info = new PackageInfo();
        info.id = "pkg-500";
        info.name = "500M 融合套餐";
        info.category = "融合套餐";
        info.monthlyFee = 79;
        info.originalFee = 129;
        info.status = "ON_SHELF";
        info.deposit = 200;
        info.deviceRent = 10;
        info.penalty = "合约期内提前解约，按剩余月份 30% 赔付违约金";
        info.slaInfo = "城区当日装当日修，超时自动赔付";

        // 2) 主图 + 轮播图
        PackageImage img1 = new PackageImage(); img1.type = "MAIN"; img1.url = "https://cdn/x/main.jpg"; img1.sortOrder = 1;
        PackageImage img2 = new PackageImage(); img2.type = "CAROUSEL"; img2.url = "https://cdn/x/c1.jpg"; img2.sortOrder = 2;
        PackageImage img3 = new PackageImage(); img3.type = "CAROUSEL"; img3.url = "https://cdn/x/c2.jpg"; img3.sortOrder = 3;
        List<PackageImage> images = Arrays.asList(img1, img2, img3);

        // 3) 融合组成
        List<PackageConvergeItem> converge = new ArrayList<>();
        converge.add(conv("宽带", "500M 高速宽带", 1));
        converge.add(conv("手机", "30GB 流量 + 500 分钟", 2));
        converge.add(conv("IPTV", "4K 超清电视", 3));
        converge.add(conv("副卡", "2 张共享副卡", 4));

        // 4) 动态可选参数
        List<PackageParamVO> params = new ArrayList<>();
        PackageParamVO bandwidth = new PackageParamVO();
        bandwidth.key = "bandwidth"; bandwidth.name = "宽带速率"; bandwidth.type = "SINGLE"; bandwidth.required = true;
        bandwidth.options = Arrays.asList(opt("300M", 0), opt("500M", 0), opt("1000M", 30));
        PackageParamVO contract = new PackageParamVO();
        contract.key = "contract"; contract.name = "合约期"; contract.type = "SINGLE"; contract.required = true;
        contract.options = Arrays.asList(opt("12个月", 0), opt("24个月", 0), opt("36个月", 0));
        PackageParamVO addon = new PackageParamVO();
        addon.key = "addon"; addon.name = "增值服务"; addon.type = "MULTI"; addon.required = false;
        addon.options = Arrays.asList(opt("FTTR全屋光纤", 30), opt("全屋WiFi", 15), opt("移动看家", 10), opt("家庭云", 10));
        params.add(bandwidth); params.add(contract); params.add(addon);

        // 5) 组装 VO
        PackageDetailVO vo = new PackageDetailVO();
        vo.id = info.id; vo.name = info.name; vo.category = info.category;
        vo.monthlyFee = info.monthlyFee; vo.originalFee = info.originalFee; vo.status = info.status;
        vo.deposit = info.deposit; vo.deviceRent = info.deviceRent; vo.penalty = info.penalty; vo.slaInfo = info.slaInfo;
        vo.images = images; vo.converge = converge; vo.params = params;

        // 6) 打印 + 演示价格计算（基础月租 + 多选加价）
        System.out.println("=== 套餐详情组装 OK ===");
        System.out.println("套餐：" + vo.name + " | 月租 ¥" + vo.monthlyFee + " | 原价 ¥" + vo.originalFee);
        System.out.println("图片：" + vo.images.size() + " 张（主图 " + vo.images.stream().filter(i -> "MAIN".equals(i.type)).count() + "）");
        System.out.println("组成：" + vo.converge.size() + " 项");
        System.out.println("参数组：" + vo.params.size() + " 组（速率/合约期/增值服务）");

        int total = vo.monthlyFee;
        String[] chosenAddons = {"FTTR全屋光纤", "家庭云"};
        for (String v : chosenAddons) {
            for (PackageParamVO g : vo.params) {
                if (!"MULTI".equals(g.type)) continue;
                for (PackageParamOption o : g.options) {
                    if (o.value.equals(v)) { total += o.extraFee; System.out.println("  + " + v + " ¥" + o.extraFee + "/月"); }
                }
            }
        }
        System.out.println("模拟选配后月费：¥" + total + "/月（含加价）");
    }

    private static PackageConvergeItem conv(String label, String desc, int sort) {
        PackageConvergeItem i = new PackageConvergeItem();
        i.label = label; i.desc = desc; i.sortOrder = sort; return i;
    }
    private static PackageParamOption opt(String value, int extraFee) {
        PackageParamOption o = new PackageParamOption();
        o.value = value; o.extraFee = extraFee; return o;
    }
}
