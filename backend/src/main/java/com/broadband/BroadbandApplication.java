package com.broadband;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 宽带业务管理系统 · 后端启动类（单体 Spring Boot）。
 *
 * <p>架构约束（已锁定）：单体 Spring Boot，不引入 Spring Cloud / 微服务 / 注册发现 / 分布式网关；
 * PC 后台由 Vue3 单页应用经 Nginx 反向代理直连本服务（默认 :8082）。</p>
 *
 * <p>业务模块 4 个（上限 5）：community（小区覆盖）/ install（派单 + 装维 SLA）/
 * product（套餐 + 流量 + 客户）/ system（RBAC 权限与运维监控）。
 * 核心算法保持纯 Java（无 Spring/ORM 依赖），可独立编译运行验证；本类只做装配。</p>
 *
 * <p>{@code annotationClass = Mapper.class} 只扫描显式标注 @Mapper 的接口，
 * 避免把各模块的 ServiceApi 接口误注册成 Mapper。</p>
 *
 * <p>排除 {@link UserDetailsServiceAutoConfiguration}：鉴权走自研 JWT 过滤器，
 * 不需要内存用户，否则启动日志会打印一个无人使用的随机密码，干扰运维排查。</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan(basePackages = "com.broadband", annotationClass = Mapper.class)
public class BroadbandApplication {

    public static void main(String[] args) {
        SpringApplication.run(BroadbandApplication.class, args);
    }
}
