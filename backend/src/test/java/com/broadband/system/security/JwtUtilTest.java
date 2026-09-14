package com.broadband.system.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtUtil 回归测试（纯 Java，不依赖 Spring 上下文）。
 *
 * <p><b>为什么必须存在</b>：本项目 JWT 为自研实现（零三方依赖）。曾出现一个隐蔽缺陷 ——
 * 数字字面量扫描误写成 {@code "-+.0-9eE".indexOf(c)}，实际只放行字符 {@code 0} 和 {@code 9}，
 * 导致 {@code iat}/{@code exp} 这类含 1~8 的时间戳解析失败；而过滤器又静默吞异常，
 * 表现为「登录成功、token 完全正确，但所有受保护接口一律 401」，排查成本极高。
 * 下面的用例锁死该行为。</p>
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-for-broadband";

    private JwtUtil util() {
        return new JwtUtil(SECRET, 7200);
    }

    @Test
    @DisplayName("签发→校验：含数字声明（iat/exp）必须能被正确解析")
    void issueAndVerifyRoundTrip() {
        JwtUtil util = util();
        String token = util.issue("admin", "U_ADMIN", "超级管理员", "信息技术部");

        Map<String, Object> claims = util.verify(token);

        assertEquals("admin", claims.get("sub"));
        assertEquals("U_ADMIN", claims.get("uid"));
        assertEquals("超级管理员", claims.get("name"));
        assertEquals("信息技术部", claims.get("dept"));
        // 关键：时间戳约 10 位、几乎必然包含 1~8，必须解析成非空数字
        assertTrue(Long.parseLong(String.valueOf(claims.get("iat"))) > 0, "iat 必须可解析");
        assertTrue(Long.parseLong(String.valueOf(claims.get("exp"))) > 0, "exp 必须可解析");
    }

    @Test
    @DisplayName("数字声明：0-9 全部数字都要能被扫描，而不是只认 0 和 9")
    void allDigitsParse() {
        for (char d = '0'; d <= '9'; d++) {
            Map<String, Object> claims = JwtUtil.parseFlatJson("{\"v\":" + d + "}");
            assertEquals(String.valueOf(d), claims.get("v"), "数字 " + d + " 应被完整解析");
        }
        // 多位数字（覆盖先前只认 0/9 的缺陷）
        assertEquals("1789384541", JwtUtil.parseFlatJson("{\"v\":1789384541}").get("v"));
        assertEquals("-42", JwtUtil.parseFlatJson("{\"v\":-42}").get("v"));
    }

    @Test
    @DisplayName("被篡改的签名必须拒绝")
    void tamperedSignatureRejected() {
        JwtUtil util = util();
        String token = util.issue("admin", "U_ADMIN", "超级管理员", "信息技术部");
        String tampered = token.substring(0, token.length() - 2) + "XY";

        assertThrows(IllegalArgumentException.class, () -> util.verify(tampered));
    }

    @Test
    @DisplayName("被篡改的载荷（改用户名）必须拒绝")
    void tamperedPayloadRejected() {
        JwtUtil util = util();
        String[] parts = util.issue("admin", "U_ADMIN", "超级管理员", "信息技术部").split("\\.");
        String forgedBody = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"hacker\",\"uid\":\"U_ADMIN\",\"name\":\"x\",\"dept\":\"y\",\"iat\":1,\"exp\":9999999999}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedBody + "." + parts[2];

        assertThrows(IllegalArgumentException.class, () -> util.verify(forged));
    }

    @Test
    @DisplayName("已过期 token 必须拒绝")
    void expiredRejected() {
        JwtUtil shortLived = new JwtUtil(SECRET, -1);   // TTL 为负 => exp 已过
        String token = shortLived.issue("admin", "U_ADMIN", "超级管理员", "信息技术部");

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> shortLived.verify(token));
        assertTrue(ex.getMessage().contains("过期"), "应提示已过期，实际：" + ex.getMessage());
    }

    @Test
    @DisplayName("不同密钥签发的 token 必须拒绝（防止密钥错配）")
    void wrongSecretRejected() {
        String token = new JwtUtil(SECRET, 7200).issue("admin", "U_ADMIN", "n", "d");
        JwtUtil other = new JwtUtil("another-secret-key-entirely", 7200);

        assertThrows(IllegalArgumentException.class, () -> other.verify(token));
        assertNotEquals(token, other.issue("admin", "U_ADMIN", "n", "d"));
    }

    @Test
    @DisplayName("格式非法的 token 必须拒绝")
    void malformedRejected() {
        assertThrows(IllegalArgumentException.class, () -> util().verify(""));
        assertThrows(IllegalArgumentException.class, () -> util().verify("only-one-part"));
        assertThrows(IllegalArgumentException.class, () -> util().verify("a.b.c"));
    }
}
