package com.broadband.product.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.product.engine.PackageUpgradeEngine;
import com.broadband.product.mapper.CustomerContractMapper;
import com.broadband.product.mapper.CustomerMapper;
import com.broadband.product.mapper.PackageMapper;
import com.broadband.product.mapper.PackageParamMapper;
import com.broadband.product.mapper.PackageParamOptionMapper;
import com.broadband.product.mapper.PackageUpgradeOrderMapper;
import com.broadband.product.model.Customer;
import com.broadband.product.model.CustomerContract;
import com.broadband.product.model.PackageInfo;
import com.broadband.product.model.PackageParam;
import com.broadband.product.model.PackageParamOption;
import com.broadband.product.model.PackageUpgradeOrder;
import com.broadband.product.model.PackageUpgradePreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐升级业务（真实数据库版）：升档选项查询 + 补差预览 + 升级申请单落库。
 *
 * <p>本类是标准的 mapper 调用方：数据访问走 MyBatis-Plus mapper，
 * 补差算法由纯 Java 的 {@link PackageUpgradeEngine} 完成（一次性补差 = 月差 × 剩余合约月数）。</p>
 *
 * <p>返回结构与小程序 pages/package/upgrade 约定保持一致：
 * {@code { current: {name, fee, contractLeftMonths}, options: [{key, type, name, extraFee}] }}，
 * 其中 type 为 {@code bandwidth} / {@code addon}。</p>
 */
@Service
public class PackageUpgradeService {

    @Autowired private CustomerMapper customerMapper;
    @Autowired private CustomerContractMapper contractMapper;
    @Autowired private PackageMapper packageMapper;
    @Autowired private PackageParamMapper paramMapper;
    @Autowired private PackageParamOptionMapper optionMapper;
    @Autowired private PackageUpgradeOrderMapper upgradeOrderMapper;

    /** 升档选项 + 当前套餐信息（从客户 / 合约 / 套餐参数组装）。 */
    public Map<String, Object> upgradeOptions(String customerId) {
        Customer customer = findCustomer(customerId);
        String packageId = resolvePackageId(customer);
        PackageInfo pkg = findPackage(packageId);
        CustomerContract contract = (customer == null) ? null : activeContract(customer.id);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("name", pkg == null ? null : pkg.name);
        current.put("fee", contract != null ? contract.monthlyFee
                : (pkg == null ? 0 : pkg.monthlyFee));
        current.put("contractLeftMonths", contract == null ? 0 : leftMonths(contract.endDate));

        List<Map<String, Object>> options = new ArrayList<>();
        if (packageId != null) {
            List<PackageParam> params = paramMapper.selectList(new QueryWrapper<PackageParam>()
                    .eq("package_id", packageId)
                    .in("group_key", Arrays.asList("bandwidth", "addon"))
                    .orderByAsc("sort_order"));
            for (PackageParam p : params) {
                String type = "bandwidth".equals(p.groupKey) ? "bandwidth" : "addon";
                for (PackageParamOption o : optionMapper.selectByParam(p.id)) {
                    options.add(opt(o.id, type, o.value, o.extraFee));
                }
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("customerId", customerId);
        resp.put("packageId", packageId);
        resp.put("current", current);
        resp.put("options", options);
        return resp;
    }

    /** 提交升档：解析选项加价 -> 补差预览 -> 落升级申请单。 */
    public PackageUpgradePreview upgrade(Map<String, Object> req) {
        String customerId = Values.str(req.get("customerId"));
        Customer customer = findCustomer(customerId);
        String packageId = resolvePackageId(customer);
        CustomerContract contract = (customer == null) ? null : activeContract(customer.id);

        int currentFee = contract != null ? contract.monthlyFee
                : (customer == null || customer.packageId == null ? 99 : packageFee(customer.packageId));
        int leftDays = (contract == null ? 0 : leftMonths(contract.endDate)) * 30;

        // 选项键 -> 月加价
        Map<String, Integer> feeIndex = optionFeeIndex(packageId);
        int targetExtra = feeOf(feeIndex, Values.str(req.get("targetBand")));
        int addonExtra = 0;
        Object addons = req.get("addons");
        if (addons instanceof List<?> list) {
            for (Object k : list) addonExtra += feeOf(feeIndex, Values.str(k));
        }

        String effectType = Values.str(req.get("effectType"));
        PackageUpgradePreview preview =
                PackageUpgradeEngine.compute(currentFee, targetExtra, addonExtra, leftDays);
        preview.effectType = effectType == null ? "immediate" : effectType;

        persistUpgrade(customerId, packageId, Values.str(req.get("targetBand")), addons, preview);
        return preview;
    }

    // ------------------------------------------------------------------ 组装辅助

    private Map<String, Object> opt(String key, String type, String name, int extraFee) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("type", type);
        m.put("name", name);
        m.put("extraFee", extraFee);
        return m;
    }

    private Customer findCustomer(String customerId) {
        if (customerId == null) return null;
        return customerMapper.selectOne(new QueryWrapper<Customer>()
                .eq("id", customerId).last("limit 1"));
    }

    /** 客户未指定套餐时回落到第一个在售套餐，保证接口始终可用。 */
    private String resolvePackageId(Customer customer) {
        if (customer != null && customer.packageId != null) return customer.packageId;
        PackageInfo p = packageMapper.selectOne(new QueryWrapper<PackageInfo>()
                .eq("status", "ON_SHELF").orderByAsc("id").last("limit 1"));
        return p == null ? null : p.id;
    }

    private PackageInfo findPackage(String packageId) {
        if (packageId == null) return null;
        return packageMapper.selectOne(new QueryWrapper<PackageInfo>()
                .eq("id", packageId).last("limit 1"));
    }

    private int packageFee(String packageId) {
        PackageInfo p = findPackage(packageId);
        return p == null ? 0 : p.monthlyFee;
    }

    private CustomerContract activeContract(String customerId) {
        return contractMapper.selectOne(new QueryWrapper<CustomerContract>()
                .eq("customer_id", customerId)
                .eq("status", "ACTIVE")
                .orderByDesc("end_date")
                .last("limit 1"));
    }

    /** 剩余合约整月数（不足一月不计，异常/空返回 0）。 */
    private int leftMonths(String endDate) {
        if (endDate == null || endDate.isEmpty()) return 0;
        try {
            long m = ChronoUnit.MONTHS.between(LocalDate.now(), LocalDate.parse(endDate));
            return (int) Math.max(0, m);
        } catch (Exception e) {
            return 0;
        }
    }

    /** 该套餐全部选项的「键 -> 月加价」索引。 */
    private Map<String, Integer> optionFeeIndex(String packageId) {
        Map<String, Integer> idx = new HashMap<>();
        if (packageId == null) return idx;
        List<PackageParam> params = paramMapper.selectList(new QueryWrapper<PackageParam>()
                .eq("package_id", packageId));
        for (PackageParam p : params) {
            for (PackageParamOption o : optionMapper.selectByParam(p.id)) {
                idx.put(o.id, o.extraFee);
            }
        }
        return idx;
    }

    private int feeOf(Map<String, Integer> idx, String key) {
        if (key == null) return 0;
        Integer v = idx.get(key);
        return v == null ? 0 : v;
    }

    private void persistUpgrade(String customerId, String fromPackageId, String targetBand,
                                Object addons, PackageUpgradePreview preview) {
        if (customerId == null) return;
        PackageUpgradeOrder order = new PackageUpgradeOrder();
        order.id = Ids.next();
        order.customerId = customerId;
        order.fromPackageId = fromPackageId;
        order.targetBandKey = targetBand;
        order.addonKeys = joinKeys(addons);
        order.effectType = preview.effectType;
        order.currentFee = preview.currentFee;
        order.monthDiff = preview.monthDiff;
        order.newFee = preview.newFee;
        order.oneTimeDiff = preview.oneTimeDiff;
        order.status = "SUBMITTED";
        order.createdTime = System.currentTimeMillis();
        upgradeOrderMapper.insert(order);
    }

    private String joinKeys(Object addons) {
        if (!(addons instanceof List<?> list) || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object o : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(Values.str(o));
        }
        return sb.toString();
    }
}
