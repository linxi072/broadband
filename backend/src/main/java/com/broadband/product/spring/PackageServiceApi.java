package com.broadband.product.spring;

import com.broadband.product.model.PackageDetailVO;

/**
 * 套餐服务接口。
 */
public interface PackageServiceApi {

    /**
     * 查询融合套餐详情（含主图/轮播图、套餐组成、动态可选参数）。
     * @param id 套餐 ID
     * @return 详情视图；不存在返回 null
     */
    PackageDetailVO getDetail(String id);
}
