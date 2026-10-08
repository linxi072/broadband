package com.broadband.system.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService 单元测试。
 *
 * <p>纯逻辑（buildDedupKey / resolveTargetRoles / render）不依赖 Spring 上下文，直接断言；
 * DB 读写（notify / create / unreadCount / markRead）以 Mockito 注入的 {@link JdbcTemplate} 验证
 * SQL 调用与参数，无需真实数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private NotificationService svc;

    // ============================================================ 纯逻辑

    @Test
    @DisplayName("buildDedupKey：维度组合 + 空值占位")
    void dedupKeyFormat() {
        assertEquals("PAY|order|O1", NotificationService.buildDedupKey("PAY", "order", "O1"));
        assertEquals("*|*|*", NotificationService.buildDedupKey(null, null, null));
        assertEquals("PAY|*|", NotificationService.buildDedupKey("PAY", null, ""));
    }

    @Test
    @DisplayName("render：占位符替换 + 缺失占位符保留")
    void renderReplacesAndKeepsMissing() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", "张三");
        p.put("amt", 100);
        assertEquals("客户 张三 支付 100 元", NotificationService.render("客户 {name} 支付 {amt} 元", p));
        assertEquals("缺失 {x}", NotificationService.render("缺失 {x}", p));
        assertEquals("空参数原样返回", NotificationService.render("空参数原样返回", null));
    }

    @Test
    @DisplayName("resolveTargetRoles：ALL/空/单角色扇出")
    void resolveRoles() {
        assertEquals(List.of("ADMIN", "OPERATOR"), NotificationService.resolveTargetRoles("ALL"));
        assertEquals(List.of("ADMIN", "OPERATOR"), NotificationService.resolveTargetRoles(null));
        assertEquals(List.of("ADMIN", "OPERATOR"), NotificationService.resolveTargetRoles("  "));
        assertEquals(List.of("OPERATOR"), NotificationService.resolveTargetRoles("operator"));
    }

    // ============================================================ 业务写入（Mock JdbcTemplate）

    @Test
    @DisplayName("notify：窗口内无重复 → 写入并返回通知ID")
    void notifyInsertsWhenNoDuplicate() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

        String id = svc.notify("PAY", "已支付", "内容", "order", "O1", "ALL");

        assertNotNull(id);
        assertTrue(id.startsWith("N"), "ID 应以 N 开头");
        verify(jdbc).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("notify：窗口内已存在相同去重键 → 跳过且不计写入")
    void notifySkipsWhenDuplicate() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);

        String id = svc.notify("PAY", "已支付", "内容", "order", "O1", "ALL");

        assertNull(id, "去重命中应返回 null");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("create：targetRole=ALL → 按 ADMIN/OPERATOR 扇出两条独立通知")
    void createFanOutPerRole() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

        List<String> ids = svc.create("合约即将到期", "请续约", "CONTRACT_EXPIRE", "contract", "C1", "ALL");

        assertEquals(2, ids.size(), "ALL 应扇出 2 条");
        assertTrue(ids.stream().allMatch(x -> x != null && x.startsWith("N")));
        verify(jdbc, org.mockito.Mockito.times(2)).update(anyString(), any(Object[].class));
    }

    // ============================================================ 查阅 / 已读

    @Test
    @DisplayName("unreadCount：直接查询未读数")
    void unreadCountQueries() {
        when(jdbc.queryForObject(anyString(), any())).thenReturn(7);
        assertEquals(7, svc.unreadCount(null));
    }

    @Test
    @DisplayName("markRead：构建 IN 子句并返回受影响行数")
    void markReadBuildsInClause() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(2);
        assertEquals(2, svc.markRead(List.of("a", "b"), null));
    }

    @Test
    @DisplayName("markRead：空 ids 直接返回 0（不触库）")
    void markReadEmptyNoOp() {
        assertEquals(0, svc.markRead(List.of(), null));
        assertEquals(0, svc.markRead(null, null));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
