package com.broadband.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.system.mapper.SysConfigMapper;
import com.broadband.system.mapper.SysDictDataMapper;
import com.broadband.system.model.SysConfig;
import com.broadband.system.model.SysDictData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数配置 / 数据字典 的内存缓存。
 *
 * <p>背景：T-05「配置动态刷新」。原 public 端点每次请求直查 DB，虽能保证「改后即时生效」，
 * 但高频读取（C 端小程序下拉、参数获取）代价高，且没有任何「热刷新」语义。本服务把
 * sys_config / sys_dict 缓存到内存：
 * <ul>
 *   <li>读：优先走缓存（懒加载——首次读自动从 DB 全量装载）；</li>
 *   <li>写：SysConfig/SysDict 的 CRUD 完成后触发对应域全量重载，下次读即反映最新值，<b>无需重启</b>；</li>
 *   <li>刷新端点：提供 {@code POST /api/system/config/refresh} 与 {@code /api/system/dict/refresh}，
 *       用于直接改库或跨节点场景手动兜底刷新。</li>
 * </ul>
 *
 * <p>配置表体量极小（数条 ~ 数十条约），全量重载代价可忽略，故采用「写后全量重载」的简洁正确策略，
 * 避免单键失效导致的缓存与 DB 不一致。
 */
@Service
public class ConfigCacheService {

    private final SysConfigMapper configMapper;
    private final SysDictDataMapper dictDataMapper;

    /** key -> {key,value,configName,configType,remark} */
    private final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();
    /** dictType -> [{label,value,sort}]（仅已启用、按 sort 升序） */
    private final Map<String, List<Map<String, Object>>> dictCache = new ConcurrentHashMap<>();

    private volatile boolean configLoaded = false;
    private volatile boolean dictLoaded = false;

    @Autowired
    public ConfigCacheService(SysConfigMapper configMapper, SysDictDataMapper dictDataMapper) {
        this.configMapper = configMapper;
        this.dictDataMapper = dictDataMapper;
    }

    // ==================================================================== 读

    /** 取参数值（不存在返回 null）。首次调用自动懒加载全量。 */
    public String getConfigValue(String key) {
        Map<String, Object> c = getConfig(key);
        return c == null ? null : str(c.get("value"));
    }

    /** 取参数完整条目（含名称/类型/备注）。首次调用自动懒加载全量（并发冷启动只全量 reload 一次）。 */
    public Map<String, Object> getConfig(String key) {
        if (!configLoaded) {
            synchronized (this) {
                if (!configLoaded) {
                    reloadConfig();
                }
            }
        }
        return configCache.get(key);
    }

    /** 取某类型下已启用字典项（首调用自动懒加载全量，并发冷启动只全量 reload 一次）。 */
    public List<Map<String, Object>> getDict(String dictType) {
        if (!dictLoaded) {
            synchronized (this) {
                if (!dictLoaded) {
                    reloadDict();
                }
            }
        }
        return dictCache.getOrDefault(dictType, List.of());
    }

    // ==================================================================== 写后/手动刷新

    /** 全量重载参数配置缓存（写后或手动触发）。 */
    public synchronized void reloadConfig() {
        Map<String, Map<String, Object>> next = new ConcurrentHashMap<>();
        for (SysConfig c : configMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", c.configKey);
            m.put("value", c.configValue);
            m.put("configName", c.configName);
            m.put("configType", c.configType);
            m.put("remark", c.remark);
            next.put(c.configKey, m);
        }
        configCache.clear();
        configCache.putAll(next);
        configLoaded = true;
    }

    /** 全量重载字典缓存（写后或手动触发）。 */
    public synchronized void reloadDict() {
        Map<String, List<Map<String, Object>>> next = new ConcurrentHashMap<>();
        for (SysDictData d : dictDataMapper.selectList(
                new QueryWrapper<SysDictData>().eq("status", "ENABLED").orderByAsc("dict_sort"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.dictLabel);
            m.put("value", d.dictValue);
            m.put("sort", d.dictSort);
            next.computeIfAbsent(d.dictType, k -> new ArrayList<>()).add(m);
        }
        dictCache.clear();
        dictCache.putAll(next);
        dictLoaded = true;
    }

    /** 全部重载。 */
    public void reloadAll() {
        reloadConfig();
        reloadDict();
    }

    // ==================================================================== 状态

    public Map<String, Object> status() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("configLoaded", configLoaded);
        s.put("dictLoaded", dictLoaded);
        s.put("configCount", configCache.size());
        s.put("dictTypeCount", dictCache.size());
        return s;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
