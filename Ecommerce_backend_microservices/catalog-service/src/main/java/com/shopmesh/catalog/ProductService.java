package com.shopmesh.catalog;

import com.shopmesh.catalog.Dtos.*;
import com.shopmesh.common.web.ApiException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ProductService {
    private final ProductRepository repo;
    private final CacheManager cacheManager;

    public ProductService(ProductRepository repo, CacheManager cacheManager) {
        this.repo = repo;
        this.cacheManager = cacheManager;
    }

    // ---------- reads (cached) ----------

    @Cacheable(cacheNames = "product", key = "#id")
    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return repo.findById(id).map(ProductDto::from)
                .orElseThrow(() -> ApiException.notFound("product " + id + " not found"));
    }

    @Cacheable(cacheNames = "productPage", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public ProductPage list(int page, int size) {
        Page<Product> p = repo.findAll(PageRequest.of(page, size, Sort.by("id")));
        List<ProductDto> items = p.getContent().stream().map(ProductDto::from).toList();
        return new ProductPage(items, page, size, p.getTotalElements(), p.getTotalPages());
    }

    // ---------- admin writes (evict cache) ----------

    @CacheEvict(cacheNames = "productPage", allEntries = true)
    @Transactional
    public ProductDto create(ProductRequest r) {
        if (repo.existsBySku(r.sku())) {
            throw ApiException.conflict("sku already exists");
        }
        return ProductDto.from(repo.save(new Product(r.sku(), r.name(), r.description(), r.price(), r.stock())));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "product", key = "#id"),
            @CacheEvict(cacheNames = "productPage", allEntries = true)})
    @Transactional
    public ProductDto update(Long id, ProductRequest r) {
        Product p = repo.findById(id).orElseThrow(() -> ApiException.notFound("product " + id + " not found"));
        p.update(r.sku(), r.name(), r.description(), r.price(), r.stock());
        return ProductDto.from(p);
    }

    // ---------- stock (used by checkout-service) ----------

    /**
     * All-or-nothing: if any line cannot be satisfied the exception rolls the whole transaction back.
     * Ids are processed in ascending order so two concurrent checkouts cannot deadlock each other.
     */
    @Transactional
    public List<ReservedLine> reserve(List<StockItem> items) {
        Map<Long, Integer> wanted = merge(items);
        List<ReservedLine> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            if (repo.decrementStock(e.getKey(), e.getValue()) == 0) {
                if (!repo.existsById(e.getKey())) {
                    throw ApiException.notFound("product " + e.getKey() + " not found");
                }
                throw ApiException.conflict("insufficient stock for product " + e.getKey());
            }
            Product p = repo.findById(e.getKey()).orElseThrow();
            lines.add(new ReservedLine(p.getId(), p.getName(), p.getPrice(), e.getValue()));
        }
        evictProductsAfterCommit(wanted.keySet());
        return lines;
    }

    /** Compensation for a failed checkout: put the reserved quantities back. */
    @Transactional
    public void release(List<StockItem> items) {
        Map<Long, Integer> wanted = merge(items);
        wanted.forEach(repo::incrementStock);
        evictProductsAfterCommit(wanted.keySet());
    }

    private static Map<Long, Integer> merge(List<StockItem> items) {
        Map<Long, Integer> m = new TreeMap<>();
        items.forEach(i -> m.merge(i.productId(), i.quantity(), Integer::sum));
        return m;
    }

    /** Evict only after the DB commit, so a concurrent read cannot re-cache the old stock value. */
    private void evictProductsAfterCommit(Collection<Long> ids) {
        Runnable evict = () -> {
            Cache c = cacheManager.getCache("product");
            if (c != null) {
                ids.forEach(c::evict);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }
}
