package com.example.redis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * DEPRECATED: Replaced by TwoTierCacheConfig
 * 
 * Original single-tier Redis cache configuration.
 * Kept for reference. Use TwoTierCacheConfig for L1 + L2 caching.
 */
// @Configuration // Disabled - using TwoTierCacheConfig instead
public class CacheConfig implements CachingConfigurer {

        public static final String CACHE_REFERENCE_DATA = "reference-data";
        public static final String CACHE_USER_DATA = "user-data";
        public static final String CACHE_API_RESPONSE = "api-response";

        @Value("${cache.ttl.reference-data:3600}")
        private long referenceDataTtl;

        @Value("${cache.ttl.user-data:300}")
        private long userDataTtl;

        @Value("${cache.ttl.api-response:30}")
        private long apiResponseTtl;

        /**
         * Redis Cache Manager with per-cache TTL configuration.
         */
        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // Default cache configuration
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                .prefixCacheNameWith("app:v1:")
                                .disableCachingNullValues();

                // Per-cache TTL configurations
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                // Reference data: Long TTL (1 hour) - product catalog, country codes, etc.
                cacheConfigurations.put(CACHE_REFERENCE_DATA,
                                defaultConfig.entryTtl(Duration.ofSeconds(referenceDataTtl)));

                // User data: Short TTL (5 minutes) - user profiles, preferences
                cacheConfigurations.put(CACHE_USER_DATA, defaultConfig.entryTtl(Duration.ofSeconds(userDataTtl)));

                // API response: Very short TTL (30 seconds) - external API responses
                cacheConfigurations.put(CACHE_API_RESPONSE, defaultConfig.entryTtl(Duration.ofSeconds(apiResponseTtl)));

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
