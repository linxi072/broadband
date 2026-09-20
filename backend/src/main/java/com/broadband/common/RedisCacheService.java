package com.broadband.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
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
    /** 并发未命中时的 per-key 互斥锁，避免缓存击穿（thundering herd）。 */
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

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
            // 缓存未命中：对同一 key 加锁，保证并发下只有一个线程回源 + 回填，
            // 其余线程在锁释放后重新读取即可命中，避免 N 倍 DB 压力与重复写缓存。
            Object lock = keyLocks.computeIfAbsent(key, k -> new Object());
            synchronized (lock) {
                try {
                    // 双重检查：持锁期间可能已被其他线程回填
                    cached = redisTemplate.opsForValue().get(key);
                    if (cached != null) {
                        return (T) cached;
                    }
                    T value = loader.get();
                    if (value != null) {
                        try {
                            redisTemplate.opsForValue().set(key, value, ttl);
                        } catch (Exception setEx) {
                            // 回填失败不影响本次返回，仅失去加速收益
                            log.warn("[cache] Redis 回填失败（已忽略），key={}，原因={}", key, setEx.getMessage());
                        }
                    }
                    return value;
                } finally {
                    keyLocks.remove(key);
                }
            }
        } catch (Exception e) {
            // 仅 Redis 读取阶段异常退化为直查；回填失败已在上面的 catch 中就地处理，不会二次回源。
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
