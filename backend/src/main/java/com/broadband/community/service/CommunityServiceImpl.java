package com.broadband.community.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.community.engine.CommunityChecker;
import com.broadband.community.mapper.CommunityDemandMapper;
import com.broadband.community.model.CommunityCheckResult;
import com.broadband.community.model.CommunityDemand;
import com.broadband.install.mapper.CommunityMapper;
import com.broadband.install.model.Community;
import com.broadband.product.mapper.PackageMapper;
import com.broadband.product.model.PackageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 小区覆盖服务实现：复用 dispatch 的小区模型与 pkg 的在售套餐，调用判定引擎。
 */
@Service
public class CommunityServiceImpl implements CommunityServiceApi {

    @Autowired private CommunityMapper communityMapper;
    @Autowired private PackageMapper packageMapper;
    @Autowired private CommunityDemandMapper demandMapper;

    @Override
    public CommunityCheckResult check(String name) {
        // 取在售套餐（可办套餐列表）
        List<PackageInfo> onShelf = packageMapper.selectList(
                new QueryWrapper<PackageInfo>().eq("status", "ON_SHELF"));

        List<Community> matched = communityMapper.selectList(
                new QueryWrapper<Community>().like("name", name == null ? "" : name).last("limit 1"));
        Community community = matched.isEmpty() ? null : matched.get(0);

        return CommunityChecker.evaluate(community, onShelf);
    }

    @Override
    public String registerDemand(CommunityDemand demand) {
        if (demand == null) demand = new CommunityDemand();
        demand.id = Ids.next();
        demand.createdAt = System.currentTimeMillis();
        if (demand.status == null || demand.status.isEmpty()) demand.status = "PENDING";
        demandMapper.insert(demand);
        return demand.id;
    }
}
