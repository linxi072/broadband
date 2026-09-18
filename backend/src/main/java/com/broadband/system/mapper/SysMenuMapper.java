package com.broadband.system.mapper;

import com.broadband.system.model.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") String roleId);

    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insertRoleMenu(@Param("roleId") String roleId, @Param("menuId") String menuId);

    // ----- 菜单节点的增删改查（不继承 BaseMapper，避免 children 被当成列）-----

    @Select("SELECT " + COLS + " FROM sys_menu WHERE id = #{id}")
    SysMenu selectById(@Param("id") String id);

    @Select("SELECT " + COLS + " FROM sys_menu WHERE parent_id = #{parentId} ORDER BY sort_order, id")
    List<SysMenu> selectChildren(@Param("parentId") String parentId);

    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId}")
    int countChildren(@Param("parentId") String parentId);

    @Insert("INSERT INTO sys_menu (id, parent_id, name, path, perm, type, sort_order) " +
            "VALUES (#{id}, #{parentId}, #{name}, #{path}, #{perm}, #{type}, #{sortOrder})")
    int insert(SysMenu m);

    @Update("UPDATE sys_menu SET parent_id=#{parentId}, name=#{name}, path=#{path}, " +
            "perm=#{perm}, type=#{type}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(SysMenu m);

    @Delete("DELETE FROM sys_menu WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteRoleMenusByMenu(@Param("menuId") String menuId);
}
