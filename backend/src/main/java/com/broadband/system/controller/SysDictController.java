package com.broadband.system.controller;

import com.broadband.system.model.SysDictData;
import com.broadband.system.model.SysDictType;
import com.broadband.system.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据字典（类型 + 数据项）。权限码 {@code system:dict} 与前端、@PreAuthorize 三处同源。
 *
 * <ul>
 *   <li>GET  /api/system/dict/types              —— 字典类型列表</li>
 *   <li>POST /api/system/dict/type               —— 新增类型</li>
 *   <li>PUT  /api/system/dict/type/{dictType}    —— 编辑类型</li>
 *   <li>DELETE /api/system/dict/type/{dictType}  —— 删除类型（级联删数据项）</li>
 *   <li>GET  /api/system/dict/data?dictType=     —— 某类型下数据项（按 dict_sort 升序）</li>
 *   <li>POST /api/system/dict/data               —— 新增数据项</li>
 *   <li>PUT  /api/system/dict/data/{id}          —— 编辑数据项</li>
 *   <li>DELETE /api/system/dict/data/{id}        —— 删除数据项</li>
 *   <li>POST /api/system/dict/refresh            —— 手动刷新字典缓存</li>
 * </ul>
 *
 * <p>本类只做权限校验与参数绑定，业务逻辑见 {@link SysDictService}。</p>
 */
@RestController
@RequestMapping("/api/system/dict")
@PreAuthorize("hasAuthority('system:dict')")
public class SysDictController {

    @Autowired private SysDictService sysDictService;

    // ---------------------------------------------------------------- 类型

    @GetMapping("/types")
    public List<SysDictType> types() {
        return sysDictService.types();
    }

    @PostMapping("/type")
    public SysDictType createType(@RequestBody Map<String, Object> req) {
        return sysDictService.createType(req);
    }

    @PutMapping("/type/{dictType}")
    public Map<String, Object> updateType(@PathVariable String dictType, @RequestBody Map<String, Object> req) {
        return sysDictService.updateType(dictType, req);
    }

    @DeleteMapping("/type/{dictType}")
    public Map<String, Object> deleteType(@PathVariable String dictType) {
        return sysDictService.deleteType(dictType);
    }

    // ---------------------------------------------------------------- 数据项

    @GetMapping("/data")
    public List<SysDictData> data(@RequestParam String dictType) {
        return sysDictService.data(dictType);
    }

    @PostMapping("/data")
    public SysDictData createData(@RequestBody Map<String, Object> req) {
        return sysDictService.createData(req);
    }

    @PutMapping("/data/{id}")
    public Map<String, Object> updateData(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysDictService.updateData(id, req);
    }

    @DeleteMapping("/data/{id}")
    public Map<String, Object> deleteData(@PathVariable String id) {
        return sysDictService.deleteData(id);
    }

    /** 手动刷新数据字典缓存（T-05 热刷新兜底端点），无需重启。 */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        return sysDictService.refresh();
    }
}
