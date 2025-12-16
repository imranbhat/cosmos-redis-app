package com.example.redis.service;

import com.example.redis.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

/**
 * External API service demonstrating very short TTL caching.
 * Uses api-response cache with 30-second TTL for external API responses.
 */
@Slf4j
@Service
public class ExternalApiService {

    private final Random random = new Random();

    /**
     * Simulated external API call with caching.
     * Very short TTL (30s) to balance freshness with backend protection.
     */
    @Cacheable(value = CacheConfig.CACHE_API_RESPONSE, key = "'exchange-rate:' + #baseCurrency + ':' + #targetCurrency")
    public Map<String, Object> getExchangeRate(String baseCurrency, String targetCurrency) {
        log.info("Cache MISS - Calling external API for exchange rate {}/{}", baseCurrency, targetCurrency);
        simulateExternalApiCall();

        // Simulated response
        double rate = 0.85 + (random.nextDouble() * 0.1);
        return Map.of(
                "base", baseCurrency,
                "target", targetCurrency,
                "rate", rate,
                "timestamp", System.currentTimeMillis());
    }

    /**
     * Simulated weather API call.
     */
    @Cacheable(value = CacheConfig.CACHE_API_RESPONSE, key = "'weather:' + #city")
    public Map<String, Object> getWeather(String city) {
        log.info("Cache MISS - Calling external weather API for {}", city);
        simulateExternalApiCall();

        return Map.of(
                "city", city,
                "temperature", 15 + random.nextInt(20),
                "humidity", 40 + random.nextInt(40),
                "condition", random.nextBoolean() ? "Sunny" : "Cloudy",
                "timestamp", System.currentTimeMillis());
    }

    private void simulateExternalApiCall() {
        try {
            // Simulate external API latency (200-500ms)
            Thread.sleep(200 + (long) (Math.random() * 300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
