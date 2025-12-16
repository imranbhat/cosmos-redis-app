package com.example.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Custom cache error handler for graceful degradation.
 * Logs errors but doesn't throw exceptions, allowing the application
 * to continue functioning when Redis is unavailable.
 */
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET error - cache: {}, key: {}, error: {}",
                cache.getName(), key, exception.getMessage());
        // Don't throw - fall through to the actual method
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT error - cache: {}, key: {}, error: {}",
                cache.getName(), key, exception.getMessage());
        // Don't throw - the method has already executed successfully
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT error - cache: {}, key: {}, error: {}",
                cache.getName(), key, exception.getMessage());
        // Don't throw - eviction failure is not critical
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.error("Cache CLEAR error - cache: {}, error: {}",
                cache.getName(), exception.getMessage());
        // Don't throw - clear failure is serious but shouldn't crash the app
    }
}
