package com.broadband.product.spring;

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
    public PackageDetailVO getDetail(String id) {
        PackageInfo info = packageMapper.selectById(id);
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

        List<PackageConvergeItem> converge = convergeMapper.selectList(
                new QueryWrapper<PackageConvergeItem>().eq("package_id", id).orderByAsc("sort_order"));
        vo.converge = converge;

        List<PackageParam> params = paramMapper.selectList(
                new QueryWrapper<PackageParam>().eq("package_id", id).orderByAsc("sort_order"));
        List<PackageParamVO> paramVOs = new ArrayList<>();
        for (PackageParam p : params) {
            PackageParamVO pv = new PackageParamVO();
            pv.key = p.groupKey;
            pv.name = p.name;
            pv.type = p.type;
            pv.required = p.required;
            List<PackageParamOption> opts = optionMapper.selectList(
                    new QueryWrapper<PackageParamOption>().eq("param_id", p.id).orderByAsc("sort_order"));
            pv.options = opts;
            paramVOs.add(pv);
        }
        vo.params = paramVOs;
        return vo;
    }
}
