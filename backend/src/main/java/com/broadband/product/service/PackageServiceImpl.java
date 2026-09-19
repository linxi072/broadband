package com.broadband.product.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.product.mapper.PackageConvergeMapper;
import com.broadband.product.mapper.PackageImageMapper;
import com.broadband.product.mapper.PackageMapper;
import com.broadband.product.mapper.PackageParamMapper;
import com.broadband.product.mapper.PackageParamOptionMapper;
import com.broadband.product.model.PackageConvergeItem;
import com.broadband.product.model.PackageDetailVO;
import com.broadband.product.model.PackageImage;
import com.broadband.product.model.PackageInfo;
import com.broadband.product.model.PackageParam;
import com.broadband.product.model.PackageParamOption;
import com.broadband.product.model.PackageParamVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 套餐服务实现：DB 聚合 -> PackageDetailVO。
 * 纯组装逻辑，可配合 DB 直接返回前端所需结构。
 */
@Service
public class PackageServiceImpl implements PackageServiceApi {

    @Autowired private PackageMapper packageMapper;
    @Autowired private PackageImageMapper imageMapper;
    @Autowired private PackageConvergeMapper convergeMapper;
    @Autowired private PackageParamMapper paramMapper;
    @Autowired private PackageParamOptionMapper optionMapper;

    @Override
    public List<Map<String, Object>> onShelfWithCover() {
        return packageMapper.selectOnShelfWithCover();
    }

    @Override
    public PackageDetailVO getDetail(String id) {
        // 显式按 id 列查询（不依赖 ORM 主键元数据，保持 model 零注解、纯 Java）
        PackageInfo info = packageMapper.selectOne(
                new QueryWrapper<PackageInfo>().eq("id", id).last("limit 1"));
        if (info == null) return null;

        PackageDetailVO vo = new PackageDetailVO();
        vo.id = info.id;
        vo.name = info.name;
        vo.category = info.category;
        vo.monthlyFee = info.monthlyFee;
        vo.originalFee = info.originalFee;
        vo.status = info.status;
        vo.deposit = info.deposit;
        vo.deviceRent = info.deviceRent;
        vo.penalty = info.penalty;
        vo.slaInfo = info.slaInfo;

        List<PackageImage> images = imageMapper.selectList(
                new QueryWrapper<PackageImage>().eq("package_id", id).orderByAsc("sort_order"));
        vo.images = images;

        // 列名 description 与接口字段 desc 的桥接在 Mapper 内完成（见 PackageConvergeMapper）
        vo.converge = convergeMapper.selectByPackage(id);

        List<PackageParam> params = paramMapper.selectList(
                new QueryWrapper<PackageParam>().eq("package_id", id).orderByAsc("sort_order"));
        List<PackageParamVO> paramVOs = new ArrayList<>();
        for (PackageParam p : params) {
            PackageParamVO pv = new PackageParamVO();
            pv.key = p.groupKey;
            pv.name = p.name;
            pv.type = p.type;
            pv.required = p.required;
            // 列名 option_value 与接口字段 value 的桥接在 Mapper 内完成（见 PackageParamOptionMapper）
            pv.options = optionMapper.selectByParam(p.id);
            paramVOs.add(pv);
        }
        vo.params = paramVOs;
        return vo;
    }
}
