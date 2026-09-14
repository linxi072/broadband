package com.broadband.install.spring;

import com.broadband.install.model.CapacityBoardItem;
import com.broadband.install.model.DispatchPlan;

import java.util.List;

/**
 * 派单服务接口（Spring 层）。核心算法见 algorithm.DispatchService。
 */
public interface DispatchServiceApi {

    /**
     * 执行派单：对待派工单池按「时段分组 -> 相邻性聚类 -> 时段容量校验 -> 贪心派单 -> 超容拦截」。
     * @return 派单方案（成功结果 + 超容异常）
     */
    DispatchPlan runDispatch();

    /**
     * 查询某时段各师傅的容量看板（已排/上限/余量/是否超容）。
     * 供 PC 派单页与小程序师傅端「排班与容量」可视化使用。
     */
    List<CapacityBoardItem> capacityBoard(String timeSlot);
}
