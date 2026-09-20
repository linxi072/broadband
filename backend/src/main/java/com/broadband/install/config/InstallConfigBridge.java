package com.broadband.install.config;

import com.broadband.install.algorithm.CapacityPolicy;
import com.broadband.system.service.ConfigCacheService;
import org.springframework.stereotype.Component;

/**
 * 桥接层：在 Spring 容器启动时把 {@link ConfigCacheService}（system 包）注入
 * {@link CapacityPolicy}（install 算法层），使容量阈值可经 sys_config 热刷新。
 *
 * <p>CapacityPolicy 保持无 Spring 依赖的纯算法类（便于独立单测），通过静态 {@code bind} 接收
 * 配置缓存引用；本组件是唯一与 Spring 容器耦合的注入点。</p>
 */
@Component
public class InstallConfigBridge {

    public InstallConfigBridge(ConfigCacheService cache) {
        CapacityPolicy.bind(cache);
    }
}
