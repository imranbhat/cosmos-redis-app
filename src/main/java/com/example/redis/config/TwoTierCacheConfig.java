package com.example.redis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Two-tier cache configuration: Caffeine (L1) + Redis (L2)
 * 
 * L1 (Caffeine): In-memory, ~0.01ms latency, per-node
 * L2 (Redis): Distributed, ~1-5ms latency, shared across nodes
 * 
 * Flow:
 * 1. Check L1 (Caffeine) - ~80% hit rate expected
 * 2. On L1 miss, check L2 (Redis)
 * 3. On L2 miss, call the actual method
 */
@Slf4j
@Configuration
public class TwoTierCacheConfig implements CachingConfigurer {

    public static final String CACHE_REFERENCE_DATA = "reference-data";
    public static final String CACHE_USER_DATA = "user-data";
    public static final String CACHE_API_RESPONSE = "api-response";

    // L1 Cache TTLs (shorter than L2)
    @Value("${cache.l1.ttl.reference-data:60}")
    private long l1ReferenceDataTtl;

    @Value("${cache.l1.ttl.user-data:30}")
    private long l1UserDataTtl;

    @Value("${cache.l1.ttl.api-response:10}")
    private long l1ApiResponseTtl;

    @Value("${cache.l1.max-size:10000}")
    private long l1MaxSize;

    // L2 Cache TTLs (Redis)
    @Value("${cache.ttl.reference-data:3600}")
    private long referenceDataTtl;

    @Value("${cache.ttl.user-data:300}")
    private long userDataTtl;

    @Value("${cache.ttl.api-response:30}")
    private long apiResponseTtl;

    /**
     * Primary cache manager - Composite of L1 (Caffeine) + L2 (Redis)
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring two-tier cache: L1 (Caffeine) + L2 (Redis)");

        // L1: Caffeine local cache
        CaffeineCacheManager caffeineCacheManager = caffeineCacheManager();

        // L2: Redis distributed cache
        RedisCacheManager redisCacheManager = redisCacheManager(connectionFactory);

        // Composite: tries L1 first, then L2
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(
                caffeineCacheManager,
                redisCacheManager);
        compositeCacheManager.setFallbackToNoOpCache(false);

        return compositeCacheManager;
    }

    /**
     * L1 Cache: Caffeine (in-memory, per-node)
     * - Ultra-fast: ~0.01ms
     * - Short TTL to stay consistent with Redis
     * - Size-limited to prevent OOM
     */
    private CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Default Caffeine cache spec
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(Duration.ofSeconds(30))
                .recordStats()); // Enable stats for monitoring

        // Per-cache configuration
        manager.registerCustomCache(CACHE_REFERENCE_DATA,
                Caffeine.newBuilder()
                        .maximumSize(l1MaxSize)
                        .expireAfterWrite(Duration.ofSeconds(l1ReferenceDataTtl))
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_USER_DATA,
                Caffeine.newBuilder()
                        .maximumSize(l1MaxSize / 2)
                        .expireAfterWrite(Duration.ofSeconds(l1UserDataTtl))
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_API_RESPONSE,
                Caffeine.newBuilder()
                        .maximumSize(l1MaxSize / 4)
                        .expireAfterWrite(Duration.ofSeconds(l1ApiResponseTtl))
                        .recordStats()
                        .build());

        log.info("L1 Cache (Caffeine) configured: maxSize={}, defaultTTL=30s", l1MaxSize);
        return manager;
    }

    /**
     * L2 Cache: Redis (distributed, shared across nodes)
     * - Fast: ~1-5ms
     * - Longer TTL than L1
     * - Consistent across all app instances
     */
    private RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .prefixCacheNameWith("app:v1:")
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(CACHE_REFERENCE_DATA,
                defaultConfig.entryTtl(Duration.ofSeconds(referenceDataTtl)));

        cacheConfigurations.put(CACHE_USER_DATA,
                defaultConfig.entryTtl(Duration.ofSeconds(userDataTtl)));

        cacheConfigurations.put(CACHE_API_RESPONSE,
                defaultConfig.entryTtl(Duration.ofSeconds(apiResponseTtl)));

        log.info("L2 Cache (Redis) configured: reference-data={}s, user-data={}s, api-response={}s",
                referenceDataTtl, userDataTtl, apiResponseTtl);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Custom error handler for graceful cache failure handling.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }
}
