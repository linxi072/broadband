package com.broadband.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 请求参数 / 查询结果值的通用转换工具。
 *
 * <p>Controller 与 Service 接收的多是 {@code Map<String, Object>}（JSON 反序列化结果或
 * JdbcTemplate 行），数值可能是 {@code Integer / Long / BigDecimal / String} 混杂。
 * 本类统一收敛这些判空与转型逻辑，避免各 Service 重复实现私有 helper。</p>
 */
public final class Values {

    private Values() {
    }

    /** 转字符串：null / 空白串统一为 {@code null}。 */
    public static String str(Object v) {
        return str(v, null);
    }

    /** 转字符串：null / 空白串回落到 {@code fallback}。 */
    public static String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? fallback : s;
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 转 int，失败或空返回 0。 */
    public static int intOf(Object v) {
        return intOf(v, 0);
    }

    /** 转 int，失败或空返回 {@code def}。 */
    public static int intOf(Object v, int def) {
        if (v == null) return def;
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 转 double，失败或空返回 0。 */
    public static double dblOf(Object v) {
        if (v == null) return 0d;
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    /** 转 Boolean：支持 true/false、1/0；无法识别返回 {@code null}。 */
    public static Boolean boolOf(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) return Boolean.FALSE;
        return null;
    }

    /** 转 Integer（保留 null 语义），失败返回 null。 */
    public static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 请求里的「ID 列表」归一：既接受 JSON 数组，也接受逗号分隔字符串。
     * 用于角色 ID 等批量关联参数的兼容处理。
     */
    public static List<String> strList(Object v) {
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) {
                String s = str(o);
                if (s != null) out.add(s);
            }
        } else {
            String s = str(v);
            if (s != null) {
                for (String x : s.split(",")) if (!x.isBlank()) out.add(x.trim());
            }
        }
        return out;
    }

    /** 逗号分隔标签串转列表（SQL 里 CONCAT_WS 拼出来的 tags 字段用）。 */
    public static List<String> splitTags(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
