package com.broadband.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.broadband.system.model.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 部门 Mapper。 */
@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    @Select("SELECT * FROM sys_department WHERE status = 'ENABLED' ORDER BY sort_order, id")
    List<SysDepartment> selectEnabled();
}
