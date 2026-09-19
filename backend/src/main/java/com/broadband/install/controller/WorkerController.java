package com.broadband.install.controller;

import com.broadband.install.service.WorkerService;
import com.broadband.system.security.WorkerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 师傅端接口（数据按登录师傅隔离）。
 *
 * <ul>
 *   <li>GET /api/worker/work-orders              —— 当前登录师傅的工单（服务端按 token 取 workerId，
 *       不信任客户端传参，杜绝「改 workerId 看别人工单」）</li>
 *   <li>GET /api/worker/work-orders/{id}         —— 工单详情；师傅只能看自己的，越权返回空</li>
 *   <li>GET /api/worker/summary                  —— 首页统计 + 今日工单</li>
 *   <li>GET /api/worker/capacity                 —— 本人各时段容量占用</li>
 *   <li>GET /api/worker/schedule                 —— 未来 7 天排班（按已派工单聚合）</li>
 *   <li>POST /api/worker/work-orders/{id}/complete —— 完工提交（测速/签名/服务项落库，置 DONE）</li>
 * </ul>
 *
 * <p>后台 ADMIN / OPERATOR 亦可调用，并可用 {@code ?workerId=} 指定查看某位师傅的工单（不传则全部）。</p>
 *
 * <p>本类只做权限校验、登录主体解析与 HTTP 响应，查询与状态流转见 {@link WorkerService}。</p>
 */
@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    @Autowired private WorkerService workerService;

    // ---------------------------------------------------------------- 工单

    @GetMapping("/work-orders")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public List<Map<String, Object>> myWorkOrders(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String workerId) {
        // 师傅端强制按自身 id 过滤，忽略客户端传的 workerId
        return workerService.workOrders(effectiveWorkerId(workerId), status);
    }

    @GetMapping("/work-orders/{id}")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> myWorkOrder(@PathVariable String id) {
        Map<String, Object> row = workerService.workOrder(id, currentWorkerId());
        return row == null ? Map.of() : row;
    }

    // ------------------------------------------------------- 首页统计（含今日工单）

    /** 首页：{@code {stats:[{n,l}], today:[{id,cust,addr,pkg,time,statusText}]}}。 */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> summary(@RequestParam(required = false) String date,
                                       @RequestParam(required = false) String workerId) {
        return workerService.summary(date, effectiveWorkerId(workerId));
    }

    // ---------------------------------------------------------------- 容量

    /**
     * 本人某天的时段容量：{@code {date, slots:[{timeSlot,time,used,max,pct}]}}。
     * 容量取自 worker_capacity.adjacent_cap；无配置时回退默认 4（相邻）/2（非相邻）。
     */
    @GetMapping("/capacity")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> myCapacity(@RequestParam(required = false) String date,
                                          @RequestParam(required = false) String workerId) {
        return workerService.capacity(date, effectiveWorkerId(workerId));
    }

    // ---------------------------------------------------------------- 排班

    /** 未来 7 天排班：{@code {days:[{date,day,am,pm,rest,count}]}}，按已派工单聚合。 */
    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> mySchedule(@RequestParam(required = false) String workerId) {
        return workerService.schedule(effectiveWorkerId(workerId));
    }

    // ---------------------------------------------------------------- 完工

    /**
     * 完工提交：校验归属 → 落测速/签名/服务项 → 置 DONE。
     * 入参 {@code {down, up, sign, services}}，返回 {@code {ok, id, status, completeTime}}。
     */
    @PostMapping("/work-orders/{id}/complete")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> complete(@PathVariable String id,
                                        @RequestBody(required = false) Map<String, Object> body) {
        return workerService.complete(id, currentWorkerId(), body);
    }

    /**
     * 开始施工：ASSIGNED -> INSTALLING，并同步业务订单状态。
     * 入参无；返回 {@code {ok, id, status}}。
     */
    @PostMapping("/work-orders/{id}/start")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> start(@PathVariable String id) {
        return workerService.start(id, currentWorkerId());
    }

    // ---------------------------------------------------------------- 内部工具

    /**
     * 生效的师傅 id：师傅登录时强制取自身 id（忽略客户端传参），后台用户取客户端指定的 workerId。
     */
    private String effectiveWorkerId(String requested) {
        String scoped = currentWorkerId();
        return scoped != null ? scoped : requested;
    }

    /** 当前登录师傅 id；后台用户（LoginUser）返回 null，表示不限范围。 */
    private String currentWorkerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        return p instanceof WorkerPrincipal wp ? wp.id : null;
    }
}
