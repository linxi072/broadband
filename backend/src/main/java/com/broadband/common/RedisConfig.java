package com.broadband.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 连接与序列化配置。
 *
 * <p>统一使用 {@code RedisTemplate<String, Object>}：key 用 String 序列化，value 用
 * {@link GenericJackson2JsonRedisSerializer}（自动写入类型信息，反序列化时能还原为原始类型，
 * 例如 {@code Map<String,Object>} / {@code List<?>} / 业务 DTO）。</p>
 *
 * <p>缓存读取的「退化」逻辑不在此处，而在 {@link RedisCacheService}：Redis 不可用 /
 * 序列化异常时自动回退到 DB 直查，保证业务不中断。</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key 统一用 String 序列化，便于在 redis-cli 里直接观察
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // value 用 Jackson JSON（带类型信息），支持任意对象缓存
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(om);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
