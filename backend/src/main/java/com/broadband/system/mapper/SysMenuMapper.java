package com.broadband.system.mapper;

import com.broadband.system.model.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 / 权限 Mapper。
 *
 * <p>刻意不继承 BaseMapper：{@link SysMenu#children} 是树形聚合用的非持久化字段，
 * 若交给 MyBatis-Plus 自动生成列清单，会把 children 当成列去查询而报错。
 * 全部查询使用显式列名（下划线→驼峰由 MyBatis 映射规则处理）。</p>
 */
@Mapper
public interface SysMenuMapper {

    String COLS = "id, parent_id, name, path, perm, type, sort_order";

    @Select("SELECT " + COLS + " FROM sys_menu ORDER BY sort_order, id")
    List<SysMenu> selectAll();

    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.name, m.path, m.perm, m.type, m.sort_order
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY m.sort_order, m.id
            """)
    List<SysMenu> selectByUser(@Param("userId") String userId);

    @Select("""
            SELECT m.id, m.parent_id, m.name, m.path, m.perm, m.type, m.sort_order
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            WHERE rm.role_id = #{roleId}
            ORDER BY m.sort_order, m.id
            """)
    List<SysMenu> selectByRole(@Param("roleId") String roleId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<String> selectMenuIds(@Param("roleId") String roleId);

    @Select("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") String roleId);

    @Select("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insertRoleMenu(@Param("roleId") String roleId, @Param("menuId") String menuId);
}
