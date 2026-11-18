package com.shopmesh.catalog;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Default TTL comes from spring.cache.redis.time-to-live (5 min). Listing pages show stock and are not
 * evicted on every checkout, so they get a short TTL instead.
 * Set CACHE_TYPE=none to turn caching off (used for before/after load tests).
 */
@Configuration
public class CacheConfig {
    @Bean
    RedisCacheManagerBuilderCustomizer cacheTtls() {
        return builder -> builder.withCacheConfiguration("productPage",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(30)));
    }
}
