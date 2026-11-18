package com.shopmesh.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Optional: SEED_PRODUCTS=200 fills an empty catalog with demo products (handy for load tests). */
@Component
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private final ProductRepository repo;
    private final int count;

    public DemoDataSeeder(ProductRepository repo, @Value("${app.seed.products:0}") int count) {
        this.repo = repo;
        this.count = count;
    }

    @Override
    public void run(String... args) {
        if (count <= 0 || repo.count() > 0) {
            return;
        }
        List<Product> batch = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            batch.add(new Product("SKU-" + String.format("%05d", i), "Demo product " + i,
                    "Demo description for product " + i, BigDecimal.valueOf(5 + (i * 7) % 195).setScale(2), 100 + i % 50));
        }
        repo.saveAll(batch);
        log.info("seeded {} demo products", count);
    }
}
