package com.broadband.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简 CSV 工具（零依赖，兼容 Excel）。
 *
 * <ul>
 *   <li>生成：字段含逗号 / 引号 / 换行时自动加引号并转义内部引号；行尾用 CRLF。</li>
 *   <li>解析：支持引号包裹字段、字段内换行、两个双引号表示一个双引号（RFC 4180 子集）。</li>
 *   <li>中文：导出时在调用方写入 UTF-8 BOM（\uFEFF），Excel 才能正确识别。</li>
 * </ul>
 */
public final class CsvUtil {

    private CsvUtil() {}

    /** 单个字段转义。 */
    public static String escape(String field) {
        if (field == null) return "";
        boolean needQuote = field.indexOf(',') >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
        if (!needQuote) return field;
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /** 多行 -> CSV 文本（CRLF 行尾）。 */
    public static String toCsv(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(escape(row[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /** CSV 文本 -> 多行（容错：忽略不完整的尾随引号状态）。 */
    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) return rows;

        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int n = text.length();

        while (i < n) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        cur.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                } else {
                    cur.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    fields.add(cur.toString());
                    cur.setLength(0);
                    i++;
                } else if (c == '\r') {
                    i++;
                } else if (c == '\n') {
                    fields.add(cur.toString());
                    rows.add(fields.toArray(new String[0]));
                    fields.clear();
                    cur.setLength(0);
                    i++;
                } else {
                    cur.append(c);
                    i++;
                }
            }
        }
        // 收尾：处理最后一行（即使没有结尾换行）
        if (cur.length() > 0 || !fields.isEmpty()) {
            fields.add(cur.toString());
            rows.add(fields.toArray(new String[0]));
        }
        return rows;
    }
}
