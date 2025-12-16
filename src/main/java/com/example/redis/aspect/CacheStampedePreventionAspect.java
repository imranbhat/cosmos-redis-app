package com.example.redis.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for preventing cache stampede on cache misses.
 * Uses semaphores to limit concurrent execution of the same cache key
 * computation.
 * 
 * Cache stampede occurs when many threads simultaneously discover a cache miss
 * and all try to regenerate the value, overwhelming the backend.
 */
@Slf4j
@Aspect
@Component
public class CacheStampedePreventionAspect {

    // Map of cache keys to semaphores (only one thread can regenerate at a time)
    private final Map<String, Semaphore> lockMap = new ConcurrentHashMap<>();

    // Maximum wait time to acquire lock (in milliseconds)
    private static final long MAX_WAIT_TIME_MS = 5000;

    /**
     * Intercept methods annotated with @Cacheable that might cause stampede.
     * This uses the method signature and arguments to create a unique lock key.
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object preventStampede(ProceedingJoinPoint joinPoint) throws Throwable {
        String lockKey = generateLockKey(joinPoint);
        Semaphore semaphore = lockMap.computeIfAbsent(lockKey, k -> new Semaphore(1));

        boolean acquired = false;
        try {
            // Try to acquire the lock with timeout
            acquired = semaphore.tryAcquire(MAX_WAIT_TIME_MS, TimeUnit.MILLISECONDS);

            if (acquired) {
                // This thread will regenerate the cache
                return joinPoint.proceed();
            } else {
                // Another thread is regenerating, wait briefly and retry
                // The @Cacheable should return the cached value on retry
                log.debug("Stampede prevention: waiting for cache regeneration - {}", lockKey);
                Thread.sleep(100);
                return joinPoint.proceed();
            }
        } finally {
            if (acquired) {
                semaphore.release();
            }
            // Clean up semaphores that are no longer needed
            cleanupSemaphore(lockKey, semaphore);
        }
    }

    private String generateLockKey(ProceedingJoinPoint joinPoint) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(joinPoint.getSignature().getDeclaringTypeName());
        keyBuilder.append(".");
        keyBuilder.append(joinPoint.getSignature().getName());

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            keyBuilder.append(":");
            keyBuilder.append(arg != null ? arg.hashCode() : "null");
        }

        return keyBuilder.toString();
    }

    private void cleanupSemaphore(String lockKey, Semaphore semaphore) {
        // Only remove if no other threads are waiting
        if (semaphore.availablePermits() == 1 && !semaphore.hasQueuedThreads()) {
            lockMap.remove(lockKey, semaphore);
        }
    }
}
