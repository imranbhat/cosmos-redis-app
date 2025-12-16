package com.example.redis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product entity for demonstration of caching.
 * Implements Serializable for Redis serialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
