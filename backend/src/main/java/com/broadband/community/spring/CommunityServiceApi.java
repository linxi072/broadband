package com.broadband.community.spring;

import com.broadband.community.model.CommunityCheckResult;
import com.broadband.community.model.CommunityDemand;

/**
 * 小区覆盖服务接口。
 */
public interface CommunityServiceApi {

    /**
     * 查询某小区是否可上门安装。
     * @param name 小区名称（支持模糊匹配）
     */
    CommunityCheckResult check(String name);

    /**
     * 登记安装需求（小区未覆盖时使用）。
     * @return 登记记录 ID
     */
    String registerDemand(CommunityDemand demand);
}
