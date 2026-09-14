package com.broadband.install.spring;

import com.broadband.install.model.CapacityBoardItem;
import com.broadband.install.model.DispatchPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 派单 REST 接口。
 *  - POST /api/dispatch/run        执行一次派单
 *  - GET  /api/dispatch/capacity   查询某时段师傅容量看板
 */
@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    @Autowired
    private DispatchServiceApi dispatchService;

    @PostMapping("/run")
    public DispatchPlan run() {
        return dispatchService.runDispatch();
    }

    @GetMapping("/capacity")
    public List<CapacityBoardItem> capacity(@RequestParam String timeSlot) {
        return dispatchService.capacityBoard(timeSlot);
    }
}
