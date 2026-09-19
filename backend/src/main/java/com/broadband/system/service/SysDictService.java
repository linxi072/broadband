package com.broadband.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.system.mapper.SysDictDataMapper;
import com.broadband.system.mapper.SysDictTypeMapper;
import com.broadband.system.model.SysDictData;
import com.broadband.system.model.SysDictType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据字典（类型 + 数据项）业务：增删改查与字典缓存刷新。
 *
 * <p>字典写操作后统一 {@link ConfigCacheService#reloadDict()}，保证 C 端开放读取拿到最新值。</p>
 */
@Service
public class SysDictService {

    private static final String RESOURCE = "/api/system/dict";

    @Autowired private SysDictTypeMapper typeMapper;
    @Autowired private SysDictDataMapper dataMapper;
    @Autowired private OperLogService operLog;
    @Autowired private ConfigCacheService configCache;

    // ---------------------------------------------------------------- 类型

    public List<SysDictType> types() {
        return typeMapper.selectList(new QueryWrapper<SysDictType>().orderByAsc("dict_name"));
    }

    public SysDictType createType(Map<String, Object> req) {
        String dictType = require(Values.str(req.get("dictType")), "字典类型编码(dictType)必填");
        if (typeMapper.selectById(dictType) != null) {
            throw new IllegalArgumentException("字典类型已存在：" + dictType);
        }
        SysDictType t = new SysDictType();
        t.dictType = dictType;
        t.dictName = require(Values.str(req.get("dictName")), "类型名称(dictName)必填");
        t.status = Values.str(req.get("status"), "ENABLED");
        t.remark = Values.str(req.get("remark"));
        t.createTime = System.currentTimeMillis();
        typeMapper.insert(t);
        configCache.reloadDict();
        log("新增字典类型 " + t.dictType);
        return t;
    }

    public Map<String, Object> updateType(String dictType, Map<String, Object> req) {
        SysDictType exist = typeMapper.selectById(dictType);
        if (exist == null) throw new IllegalArgumentException("字典类型不存在：" + dictType);
        SysDictType patch = new SysDictType();
        patch.dictType = dictType;
        patch.dictName = Values.str(req.get("dictName"), exist.dictName);
        patch.status = Values.str(req.get("status"), exist.status);
        patch.remark = Values.str(req.get("remark"), exist.remark);
        typeMapper.updateById(patch);
        configCache.reloadDict();
        log("编辑字典类型 " + dictType);
        return Map.of("ok", true);
    }

    /** 删除字典类型（级联删除其下全部数据项）。 */
    public Map<String, Object> deleteType(String dictType) {
        dataMapper.delete(new QueryWrapper<SysDictData>().eq("dict_type", dictType));
        typeMapper.deleteById(dictType);
        configCache.reloadDict();
        log("删除字典类型 " + dictType);
        return Map.of("ok", true);
    }

    // ---------------------------------------------------------------- 数据项

    public List<SysDictData> data(String dictType) {
        return dataMapper.selectList(new QueryWrapper<SysDictData>()
                .eq("dict_type", dictType).orderByAsc("dict_sort"));
    }

    public SysDictData createData(Map<String, Object> req) {
        String dictType = require(Values.str(req.get("dictType")), "dictType 必填");
        if (typeMapper.selectById(dictType) == null) {
            throw new IllegalArgumentException("字典类型不存在：" + dictType);
        }
        SysDictData d = new SysDictData();
        d.id = Ids.next();
        d.dictType = dictType;
        d.dictLabel = require(Values.str(req.get("dictLabel")), "展示标签(dictLabel)必填");
        d.dictValue = require(Values.str(req.get("dictValue")), "值(dictValue)必填");
        d.dictSort = Values.intOf(req.get("dictSort"));
        d.status = Values.str(req.get("status"), "ENABLED");
        d.remark = Values.str(req.get("remark"));
        d.createTime = System.currentTimeMillis();
        dataMapper.insert(d);
        configCache.reloadDict();
        log("新增字典数据项 " + d.dictType + "/" + d.dictLabel);
        return d;
    }

    public Map<String, Object> updateData(String id, Map<String, Object> req) {
        SysDictData exist = dataMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("字典数据项不存在：" + id);
        SysDictData patch = new SysDictData();
        patch.id = id;
        patch.dictType = Values.str(req.get("dictType"), exist.dictType);
        patch.dictLabel = Values.str(req.get("dictLabel"), exist.dictLabel);
        patch.dictValue = Values.str(req.get("dictValue"), exist.dictValue);
        patch.dictSort = Values.intOf(req.get("dictSort"), exist.dictSort);
        patch.status = Values.str(req.get("status"), exist.status);
        patch.remark = Values.str(req.get("remark"), exist.remark);
        dataMapper.updateById(patch);
        configCache.reloadDict();
        log("编辑字典数据项 " + id);
        return Map.of("ok", true);
    }

    public Map<String, Object> deleteData(String id) {
        dataMapper.deleteById(id);
        configCache.reloadDict();
        log("删除字典数据项 " + id);
        return Map.of("ok", true);
    }

    /** 手动刷新字典缓存（T-05 热刷新兜底，适用于直接改库或跨节点强制同步）。 */
    public Map<String, Object> refresh() {
        configCache.reloadDict();
        return configCache.status();
    }

    // ------------------------------------------------------------------ 辅助

    private static String require(String v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }

    private void log(String action) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, RESOURCE, "WRITE", "-", "成功", 0);
    }
}
