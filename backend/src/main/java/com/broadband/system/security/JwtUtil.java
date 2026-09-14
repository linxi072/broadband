package com.broadband.system.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简 JWT（HS256）实现 —— 纯 Java，零三方依赖，可脱离 Spring 单独编译运行与单测。
 *
 * <p>选择自研而非引入 jjwt 的原因：① 依赖面最小，符合「算法与框架解耦」的项目原则；
 * ② 只需要签发/校验两个动作，标准库的 Mac + Base64 足够；③ payload 只承载会话标识
 * （账号、用户ID、姓名、部门、签发与过期时间），角色与权限码每次请求实时从库中读取，
 * 保证「改角色即时生效」，避免把权限固化进 token。</p>
 *
 * <p>token 结构：base64url(header).base64url(payload).base64url(HMAC-SHA256(前两段))</p>
 */
public class JwtUtil {

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final long ttlSeconds;

    public JwtUtil(String secret, long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    /** 签发 token。 */
    public String issue(String username, String userId, String name, String dept) {
        long now = System.currentTimeMillis() / 1000L;
        StringBuilder p = new StringBuilder(160);
        p.append('{')
         .append("\"sub\":").append(quote(username))
         .append(",\"uid\":").append(quote(userId))
         .append(",\"name\":").append(quote(name))
         .append(",\"dept\":").append(quote(dept))
         .append(",\"iat\":").append(now)
         .append(",\"exp\":").append(now + ttlSeconds)
         .append('}');

        String head = enc(HEADER.getBytes(StandardCharsets.UTF_8));
        String body = enc(p.toString().getBytes(StandardCharsets.UTF_8));
        String data = head + "." + body;
        return data + "." + enc(sign(data));
    }

    /**
     * 校验并解析 token。
     *
     * @throws IllegalArgumentException 签名不符 / 格式错误 / 已过期
     */
    public Map<String, Object> verify(String token) {
        if (token == null || token.isEmpty()) throw new IllegalArgumentException("token 为空");
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("token 格式错误");

        String data = parts[0] + "." + parts[1];
        if (!constantTimeEquals(parts[2], enc(sign(data)))) {
            throw new IllegalArgumentException("token 签名校验失败");
        }

        String json = new String(DEC.decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = parseFlatJson(json);

        Object exp = claims.get("exp");
        long expSec = exp == null ? 0L : Long.parseLong(String.valueOf(exp));
        if (expSec > 0 && System.currentTimeMillis() / 1000L > expSec) {
            throw new IllegalArgumentException("token 已过期");
        }
        return claims;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    // ------------------------------------------------------------------ 内部实现

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败: " + e.getMessage(), e);
        }
    }

    private static String enc(byte[] b) {
        return ENC.encodeToString(b);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }

    private static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    /** 解析「扁平的」JSON 对象（值仅支持字符串/数字/null），够 JWT 声明使用。 */
    static Map<String, Object> parseFlatJson(String json) {
        Map<String, Object> out = new LinkedHashMap<>();
        int i = 0;
        int n = json.length();
        while (i < n && json.charAt(i) != '{') i++;
        i++;
        while (i < n) {
            while (i < n && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
            if (i >= n || json.charAt(i) == '}') break;
            if (json.charAt(i) != '"') throw new IllegalArgumentException("JSON 键格式错误");
            StringBuilder key = new StringBuilder();
            i = readString(json, i, key);
            while (i < n && json.charAt(i) != ':') i++;
            i++;
            while (i < n && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= n) break;

            char c = json.charAt(i);
            if (c == '"') {
                StringBuilder val = new StringBuilder();
                i = readString(json, i, val);
                out.put(key.toString(), val.toString());
            } else if (c == 'n') {
                out.put(key.toString(), null);
                i += 4;
            } else {
                int s = i;
                // 数字字面量：数字/正负号/小数点/指数符号。
                // 注意：不能写成 "-+.0-9eE".indexOf(c)，那只是「若干单个字符」而不是 0-9 区间，
                // 会让 1~8 无法被识别，导致 iat/exp 解析失败（曾造成「token 正确却始终 401」）。
                while (i < n && isNumberChar(json.charAt(i))) i++;
                out.put(key.toString(), json.substring(s, i));
            }
        }
        return out;
    }

    /** 是否为 JSON 数字字面量允许的字符。 */
    private static boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E';
    }

    /** 从 json[start] 的引号开始读取字符串，返回结束引号后的下标。 */    private static int readString(String json, int start, StringBuilder sb) {
        int i = start + 1;
        int n = json.length();
        while (i < n) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < n) {
                char e = json.charAt(++i);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    default -> sb.append(e);
                }
                i++;
            } else if (c == '"') {
                return i + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return i;
    }
}
