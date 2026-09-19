package com.broadband.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 通用 Redis 缓存读取服务（fail-safe）。
 *
 * <p>设计原则：缓存是「加速层」而非「数据源」。任何 Redis 异常（连接失败、序列化失败、
 * 键不存在之外的错误）都只打 WARN 日志并退化为 {@code loader.get()} 直查 DB，绝不向上抛错，
 * 因此 Redis 宕机/未部署时业务照常运行（仅失去加速收益）。</p>
 *
 * <p>典型用法（读密集、对实时性要求不高的聚合结果）：</p>
 * <pre>{@code
 * return cache.get("dashboard:stats", Duration.ofSeconds(60), () -> computeStats());
 * }</pre>
 *
 * <p>{@code app.cache.enabled=false} 时（无 Redis 环境）直接走 loader，等价于不缓存。</p>
 */
@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.enabled:false}")
    private boolean enabled;

    public RedisCacheService(@org.springframework.beans.factory.annotation.Autowired(required = false)
                                     RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读缓存；未命中则执行 loader 并回填。Redis 异常时退化为 loader。
     *
     * @param key  缓存键（建议带业务前缀，如 {@code sla:dashboard}）
     * @param ttl  缓存有效期
     * @param loader 缓存未命中时的数据加载器（通常是一次 DB 聚合查询）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Duration ttl, Supplier<T> loader) {
        if (!enabled || redisTemplate == null) {
            return loader.get();
        }
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return (T) cached;
            }
            T value = loader.get();
            if (value != null) {
                redisTemplate.opsForValue().set(key, value, ttl);
            }
            return value;
        } catch (Exception e) {
            log.warn("[cache] Redis 读取失败，退化为直查 DB，key={}，原因={}", key, e.getMessage());
            return loader.get();
        }
    }

    /** 主动失效某个缓存键（数据变更后调用，避免缓存与 DB 不一致）。 */
    public void evict(String key) {
        if (!enabled || redisTemplate == null) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[cache] Redis 失效失败，key={}，原因={}", key, e.getMessage());
        }
    }
}
