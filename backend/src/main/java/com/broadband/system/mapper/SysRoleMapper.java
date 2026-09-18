package com.broadband.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.broadband.system.model.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 角色 Mapper。 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT * FROM sys_role WHERE code = #{code} LIMIT 1")
    SysRole selectByCode(@Param("code") String code);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") String roleId);

    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteUserRoles(@Param("roleId") String roleId);
}
