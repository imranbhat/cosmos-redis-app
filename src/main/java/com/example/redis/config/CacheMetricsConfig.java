package com.example.redis.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for cache and Redis observability metrics.
 * Exposes cache hit/miss rates, latency, and connection pool metrics.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheMetricsConfig {

    /**
     * Customize meter registry with application tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", "spring-boot-redis-app")
                .commonTags("framework", "spring-boot");
    }

    /**
     * Note: Spring Boot 3.x automatically registers Redis metrics when:
     * - spring-boot-starter-actuator is present
     * - micrometer-registry-prometheus is present
     * - management.endpoints.web.exposure.include=prometheus
     * 
     * Metrics available at /actuator/prometheus:
     * - cache_gets_total (hits/misses)
     * - cache_puts_total
     * - cache_evictions_total
     * - lettuce_command_completion_seconds
     * - lettuce_command_firstresponse_seconds
     * - redis_*
     */
}
