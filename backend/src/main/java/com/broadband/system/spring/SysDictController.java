package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.system.mapper.SysDictDataMapper;
import com.broadband.system.mapper.SysDictTypeMapper;
import com.broadband.system.model.SysDictData;
import com.broadband.system.model.SysDictType;
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
 *   <li>GET  /api/dict/public/{dictType}         —— 开放读取（已启用数据项，供 C 端小程序下拉，免鉴权）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/system/dict")
@PreAuthorize("hasAuthority('system:dict')")
public class SysDictController {

    @Autowired private SysDictTypeMapper typeMapper;
    @Autowired private SysDictDataMapper dataMapper;
    @Autowired private OperLogService operLog;

    // ---------------------------------------------------------------- 类型

    @GetMapping("/types")
    public List<SysDictType> types() {
        return typeMapper.selectList(new QueryWrapper<SysDictType>().orderByAsc("dict_name"));
    }

    @PostMapping("/type")
    public SysDictType createType(@RequestBody Map<String, Object> req) {
        String dictType = require(str(req.get("dictType")), "字典类型编码(dictType)必填");
        if (typeMapper.selectById(dictType) != null) {
            throw new IllegalArgumentException("字典类型已存在：" + dictType);
        }
        SysDictType t = new SysDictType();
        t.dictType = dictType;
        t.dictName = require(str(req.get("dictName")), "类型名称(dictName)必填");
        t.status = str(req.get("status")) == null ? "ENABLED" : str(req.get("status"));
        t.remark = str(req.get("remark"));
        t.createTime = System.currentTimeMillis();
        typeMapper.insert(t);
        log("新增字典类型 " + t.dictType);
        return t;
    }

    @PutMapping("/type/{dictType}")
    public Map<String, Object> updateType(@PathVariable String dictType, @RequestBody Map<String, Object> req) {
        SysDictType exist = typeMapper.selectById(dictType);
        if (exist == null) throw new IllegalArgumentException("字典类型不存在：" + dictType);
        SysDictType patch = new SysDictType();
        patch.dictType = dictType;
        patch.dictName = str(req.get("dictName")) == null ? exist.dictName : str(req.get("dictName"));
        patch.status = str(req.get("status")) == null ? exist.status : str(req.get("status"));
        patch.remark = str(req.get("remark")) == null ? exist.remark : str(req.get("remark"));
        typeMapper.updateById(patch);
        log("编辑字典类型 " + dictType);
        return Map.of("ok", true);
    }

    @DeleteMapping("/type/{dictType}")
    public Map<String, Object> deleteType(@PathVariable String dictType) {
        dataMapper.delete(new QueryWrapper<SysDictData>().eq("dict_type", dictType));
        typeMapper.deleteById(dictType);
        log("删除字典类型 " + dictType);
        return Map.of("ok", true);
    }

    // ---------------------------------------------------------------- 数据项

    @GetMapping("/data")
    public List<SysDictData> data(@RequestParam String dictType) {
        return dataMapper.selectList(new QueryWrapper<SysDictData>()
                .eq("dict_type", dictType).orderByAsc("dict_sort"));
    }

    @PostMapping("/data")
    public SysDictData createData(@RequestBody Map<String, Object> req) {
        String dictType = require(str(req.get("dictType")), "dictType 必填");
        if (typeMapper.selectById(dictType) == null) {
            throw new IllegalArgumentException("字典类型不存在：" + dictType);
        }
        SysDictData d = new SysDictData();
        d.id = Ids.next();
        d.dictType = dictType;
        d.dictLabel = require(str(req.get("dictLabel")), "展示标签(dictLabel)必填");
        d.dictValue = require(str(req.get("dictValue")), "值(dictValue)必填");
        d.dictSort = asInt(req.get("dictSort"), 0);
        d.status = str(req.get("status")) == null ? "ENABLED" : str(req.get("status"));
        d.remark = str(req.get("remark"));
        d.createTime = System.currentTimeMillis();
        dataMapper.insert(d);
        log("新增字典数据项 " + d.dictType + "/" + d.dictLabel);
        return d;
    }

    @PutMapping("/data/{id}")
    public Map<String, Object> updateData(@PathVariable String id, @RequestBody Map<String, Object> req) {
        SysDictData exist = dataMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("字典数据项不存在：" + id);
        SysDictData patch = new SysDictData();
        patch.id = id;
        patch.dictType = str(req.get("dictType")) == null ? exist.dictType : str(req.get("dictType"));
        patch.dictLabel = str(req.get("dictLabel")) == null ? exist.dictLabel : str(req.get("dictLabel"));
        patch.dictValue = str(req.get("dictValue")) == null ? exist.dictValue : str(req.get("dictValue"));
        patch.dictSort = req.get("dictSort") == null ? exist.dictSort : asInt(req.get("dictSort"), exist.dictSort);
        patch.status = str(req.get("status")) == null ? exist.status : str(req.get("status"));
        patch.remark = str(req.get("remark")) == null ? exist.remark : str(req.get("remark"));
        dataMapper.updateById(patch);
        log("编辑字典数据项 " + id);
        return Map.of("ok", true);
    }

    @DeleteMapping("/data/{id}")
    public Map<String, Object> deleteData(@PathVariable String id) {
        dataMapper.deleteById(id);
        log("删除字典数据项 " + id);
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ 辅助

    private void log(String action) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username,
                me == null ? null : me.user.name,
                action, "/api/system/dict", "WRITE", "-", "成功", 0);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String require(String v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }

    private static int asInt(Object v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}
