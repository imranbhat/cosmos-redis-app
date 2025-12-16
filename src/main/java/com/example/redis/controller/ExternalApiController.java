package com.example.redis.controller;

import com.example.redis.service.ExternalApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API demonstrating external API response caching with very short TTL.
 */
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    /**
     * Get exchange rate - cached for 30 seconds.
     * Protects external API from excessive calls.
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "EUR") String target) {
        return ResponseEntity.ok(externalApiService.getExchangeRate(base, target));
    }

    /**
     * Get weather - cached for 30 seconds.
     */
    @GetMapping("/weather/{city}")
    public ResponseEntity<Map<String, Object>> getWeather(@PathVariable String city) {
        return ResponseEntity.ok(externalApiService.getWeather(city));
    }
}
