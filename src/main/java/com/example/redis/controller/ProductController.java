package com.example.redis.controller;

import com.example.redis.model.Product;
import com.example.redis.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for product operations demonstrating cache behavior.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Get product by ID - demonstrates read-through cache.
     * First call is slow (DB hit), subsequent calls are fast (cache hit).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all products - not cached to avoid memory issues.
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    /**
     * Create product - demonstrates write-through cache.
     * New product is immediately available in cache.
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update product - demonstrates cache put.
     * Cache is updated with new value.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productService.findById(id)
                .map(existing -> {
                    product.setId(id);
                    product.setCreatedAt(existing.getCreatedAt());
                    return ResponseEntity.ok(productService.save(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete product - demonstrates cache eviction.
     * Entry is removed from cache.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear product cache - admin operation.
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache() {
        productService.clearCache();
        return ResponseEntity.noContent().build();
    }
}
