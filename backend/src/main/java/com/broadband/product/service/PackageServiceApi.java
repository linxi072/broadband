package com.broadband.product.service;

import com.broadband.product.model.PackageDetailVO;

import java.util.List;
import java.util.Map;

/**
 * 套餐服务接口。
 */
public interface PackageServiceApi {

    /**
     * 在售套餐列表（含主图），供小程序套餐列表页使用。
     * @return 套餐行列表（id / name / monthlyFee / cover 等）
     */
    List<Map<String, Object>> onShelfWithCover();

    /**
     * 查询融合套餐详情（含主图/轮播图、套餐组成、动态可选参数）。
     * @param id 套餐 ID
     * @return 详情视图；不存在返回 null
     */
    PackageDetailVO getDetail(String id);
}
