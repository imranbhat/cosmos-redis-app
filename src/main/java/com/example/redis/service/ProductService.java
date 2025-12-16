package com.example.redis.service;

import com.example.redis.config.CacheConfig;
import com.example.redis.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Product service demonstrating read-through, write-through, and cache eviction
 * patterns.
 * Uses reference-data cache with long TTL (1 hour).
 */
@Slf4j
@Service
public class ProductService {

    // Simulated database storage
    private final Map<Long, Product> productDatabase = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public ProductService() {
        // Initialize with sample data
        initializeSampleData();
    }

    /**
     * Read-through cache pattern.
     * First call fetches from "database", subsequent calls return cached value.
     * 
     * @param id Product ID
     * @return Product if found
     */
    @Cacheable(value = CacheConfig.CACHE_REFERENCE_DATA, key = "'product:' + #id", unless = "#result == null")
    public Optional<Product> findById(Long id) {
        log.info("Cache MISS - Fetching product {} from database", id);
        simulateDbLatency();
        return Optional.ofNullable(productDatabase.get(id));
    }

    /**
     * Write-through cache pattern.
     * Updates cache after successful database write.
     * 
     * @param product Product to save
     * @return Saved product
     */
    @CachePut(value = CacheConfig.CACHE_REFERENCE_DATA, key = "'product:' + #result.id")
    public Product save(Product product) {
        log.info("Saving product to database: {}", product.getName());
        simulateDbLatency();

        if (product.getId() == null) {
            product.setId(idGenerator.incrementAndGet());
            product.setCreatedAt(LocalDateTime.now());
        }
        product.setUpdatedAt(LocalDateTime.now());

        productDatabase.put(product.getId(), product);
        return product;
    }

    /**
     * Cache eviction on delete.
     * Removes entry from cache when deleted from database.
     * 
     * @param id Product ID to delete
     */
    @CacheEvict(value = CacheConfig.CACHE_REFERENCE_DATA, key = "'product:' + #id")
    public void delete(Long id) {
        log.info("Deleting product {} from database", id);
        simulateDbLatency();
        productDatabase.remove(id);
    }

    /**
     * Bulk operation - no caching for lists (to avoid memory issues).
     * 
     * @return All products
     */
    public List<Product> findAll() {
        log.info("Fetching all products from database");
        simulateDbLatency();
        return List.copyOf(productDatabase.values());
    }

    /**
     * Clear all products from cache.
     */
    @CacheEvict(value = CacheConfig.CACHE_REFERENCE_DATA, allEntries = true)
    public void clearCache() {
        log.info("Clearing all product cache entries");
    }

    private void simulateDbLatency() {
        try {
            // Simulate database latency (100-200ms)
            Thread.sleep(100 + (long) (Math.random() * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initializeSampleData() {
        save(Product.builder()
                .name("Laptop Pro")
                .description("High-performance laptop")
                .category("Electronics")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(50)
                .active(true)
                .build());

        save(Product.builder()
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .category("Electronics")
                .price(new BigDecimal("29.99"))
                .stockQuantity(200)
                .active(true)
                .build());

        save(Product.builder()
                .name("USB-C Hub")
                .description("7-port USB-C hub")
                .category("Accessories")
                .price(new BigDecimal("49.99"))
                .stockQuantity(100)
                .active(true)
                .build());
    }
}
