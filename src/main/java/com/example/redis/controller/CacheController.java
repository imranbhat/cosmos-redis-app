package com.example.redis.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Properties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cache management endpoints for operations and monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;

    /**
     * Get cache statistics and info.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        // List all cache names
        stats.put("cacheNames", cacheManager.getCacheNames());

        // Redis connection info
        try {
            var connection = connectionFactory.getConnection();
            var serverCommands = connection.serverCommands();
            Properties info = serverCommands.info("memory");
            if (info != null) {
                stats.put("redisMemory", info.toString());
            }
            connection.close();
        } catch (Exception e) {
            stats.put("redisStatus", "Error: " + e.getMessage());
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * List keys matching a pattern (for debugging).
     */
    @GetMapping("/keys")
    public ResponseEntity<Set<String>> getKeys(@RequestParam(defaultValue = "app:v1:*") String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return ResponseEntity.ok(keys);
    }

    /**
     * Clear a specific cache.
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cache '{}' cleared", cacheName);
            return ResponseEntity.ok(Map.of("message", "Cache '" + cacheName + "' cleared"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Clear all caches.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        log.info("All caches cleared");
        return ResponseEntity.ok(Map.of("message", "All caches cleared"));
    }

    /**
     * Health check for Redis connectivity.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        try {
            var connection = connectionFactory.getConnection();
            String pong = connection.ping();
            health.put("status", "UP");
            health.put("redis", "Connected");
            health.put("ping", pong);
            connection.close();
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return ResponseEntity.ok(health);
    }
}
