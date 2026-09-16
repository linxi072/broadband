package com.broadband.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.broadband.system.model.SysRole;
import com.broadband.system.model.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 系统用户 Mapper（含按用户取角色 / 权限码的联表查询）。 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    /** 用户拥有的角色（含 code 与 name，用于 ROLE_ 前缀与前端展示）。 */
    @Select("""
            SELECT r.* FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<SysRole> selectRoles(@Param("userId") String userId);

    /**
     * 用户拥有的权限码（去重）。这就是 Spring Security 的 authority 集合，
     * 同时也是前端路由 meta.perm 的来源 —— 前后端同一份数据。
     */
    @Select("""
            SELECT DISTINCT m.perm FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.perm IS NOT NULL AND m.perm <> ''
            ORDER BY m.perm
            """)
    List<String> selectPerms(@Param("userId") String userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") String userId);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") String userId, @Param("roleId") String roleId);
}
