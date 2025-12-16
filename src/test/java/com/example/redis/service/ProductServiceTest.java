package com.example.redis.service;

import com.example.redis.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProductService.
 * Uses simple cache (not Redis) for testing without Redis dependency.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void shouldReturnProductById() {
        // Given: product with ID 1 exists (from sample data)

        // When
        Optional<Product> product = productService.findById(1L);

        // Then
        assertThat(product).isPresent();
        assertThat(product.get().getName()).isEqualTo("Laptop Pro");
    }

    @Test
    void shouldSaveNewProduct() {
        // Given
        Product newProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .category("Test")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .active(true)
                .build();

        // When
        Product saved = productService.save(newProduct);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldReturnAllProducts() {
        // When
        var products = productService.findAll();

        // Then
        assertThat(products).hasSizeGreaterThanOrEqualTo(3); // Sample data
    }

    @Test
    void shouldDeleteProduct() {
        // Given
        Product product = productService.save(Product.builder()
                .name("To Delete")
                .category("Test")
                .price(BigDecimal.TEN)
                .build());

        // When
        productService.delete(product.getId());

        // Then
        assertThat(productService.findById(product.getId())).isEmpty();
    }
}
