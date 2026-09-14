package com.broadband.common;

import java.util.UUID;

/**
 * 主键生成工具（纯 Java，无框架依赖）。
 *
 * <p>系统内所有业务主键均为字符串。这里显式生成主键而不依赖 ORM 的主键策略，
 * 目的是让「无需 ORM 也能跑」的纯 Java 演示/单测路径同样可用。</p>
 */
public final class Ids {

    private Ids() {}

    /** 生成 24 位十六进制字符串主键。 */
    public static String next() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
