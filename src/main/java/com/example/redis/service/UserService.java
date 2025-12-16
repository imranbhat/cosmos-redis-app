package com.example.redis.service;

import com.example.redis.config.CacheConfig;
import com.example.redis.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User service demonstrating short-TTL user data caching.
 * Uses user-data cache with 5-minute TTL.
 */
@Slf4j
@Service
public class UserService {

    // Simulated database storage
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public UserService() {
        initializeSampleData();
    }

    /**
     * Get user by ID with caching.
     * Short TTL ensures user data stays relatively fresh.
     */
    @Cacheable(value = CacheConfig.CACHE_USER_DATA, key = "'user:' + #id", unless = "#result == null")
    public Optional<User> findById(Long id) {
        log.info("Cache MISS - Fetching user {} from database", id);
        simulateDbLatency();
        return Optional.ofNullable(userDatabase.get(id));
    }

    /**
     * Update user with cache refresh.
     */
    @CachePut(value = CacheConfig.CACHE_USER_DATA, key = "'user:' + #result.id")
    public User save(User user) {
        log.info("Saving user to database: {}", user.getUsername());
        simulateDbLatency();

        if (user.getId() == null) {
            user.setId(idGenerator.incrementAndGet());
            user.setCreatedAt(LocalDateTime.now());
        }

        userDatabase.put(user.getId(), user);
        return user;
    }

    /**
     * Update last login and refresh cache.
     */
    @CachePut(value = CacheConfig.CACHE_USER_DATA, key = "'user:' + #id")
    public User updateLastLogin(Long id) {
        User user = userDatabase.get(id);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userDatabase.put(id, user);
        }
        return user;
    }

    /**
     * Delete user and evict from cache.
     */
    @CacheEvict(value = CacheConfig.CACHE_USER_DATA, key = "'user:' + #id")
    public void delete(Long id) {
        log.info("Deleting user {} from database", id);
        userDatabase.remove(id);
    }

    private void simulateDbLatency() {
        try {
            Thread.sleep(50 + (long) (Math.random() * 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initializeSampleData() {
        save(User.builder()
                .username("john.doe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .build());

        save(User.builder()
                .username("jane.smith")
                .email("jane.smith@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .active(true)
                .build());
    }
}
