package com.broadband.system.controller;

import com.broadband.system.model.SysOperLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.broadband.system.service.OperLogService;

/** 操作日志查询。 */
@RestController
@RequestMapping("/api/system/logs")
@PreAuthorize("hasAuthority('system:log')")
public class SysOperLogController {

    @Autowired private OperLogService operLog;

    @GetMapping
    public List<SysOperLog> list(@RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "100") int size) {
        return operLog.recent(keyword, size);
    }
}
