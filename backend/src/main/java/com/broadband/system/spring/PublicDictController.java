package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.system.mapper.SysDictDataMapper;
import com.broadband.system.model.SysDictData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典开放读取接口（免鉴权）。供 C 端小程序下拉框、参数获取使用。
 *
 * <ul>
 *   <li>GET /api/dict/public/{dictType} —— 返回某类型下「已启用」数据项 [{label,value,sort}]</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dict/public")
public class PublicDictController {

    @Autowired private SysDictDataMapper dataMapper;

    @GetMapping("/{dictType}")
    public List<Map<String, Object>> publicData(@PathVariable String dictType) {
        List<SysDictData> list = dataMapper.selectList(new QueryWrapper<SysDictData>()
                .eq("dict_type", dictType).eq("status", "ENABLED").orderByAsc("dict_sort"));
        return list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.dictLabel);
            m.put("value", d.dictValue);
            m.put("sort", d.dictSort);
            return m;
        }).toList();
    }
}
