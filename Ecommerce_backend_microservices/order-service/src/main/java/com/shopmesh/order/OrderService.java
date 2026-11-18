package com.shopmesh.order;

import com.shopmesh.common.web.ApiException;
import com.shopmesh.order.Dtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository repo;

    public OrderService(OrderRepository repo) {
        this.repo = repo;
    }

    /** Idempotent: the same (user, idempotencyKey) always returns the same order. */
    @Transactional
    public OrderDto create(long userId, CreateOrderRequest r) {
        Optional<CustomerOrder> existing = repo.findByUserIdAndIdempotencyKey(userId, r.idempotencyKey());
        if (existing.isPresent()) {
            return toDto(existing.get());
        }
        BigDecimal total = r.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CustomerOrder order = new CustomerOrder(userId, "CREATED", total, r.idempotencyKey());
        r.items().forEach(i -> order.getItems().add(new OrderItem(i.productId(), i.productName(), i.unitPrice(), i.quantity())));
        return toDto(repo.save(order));
    }

    @Transactional(readOnly = true)
    public Optional<OrderDto> findByKey(long userId, String key) {
        return repo.findByUserIdAndIdempotencyKey(userId, key).map(OrderService::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDto get(long userId, long id) {
        return repo.findByIdAndUserId(id, userId).map(OrderService::toDto)
                .orElseThrow(() -> ApiException.notFound("order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> list(long userId, int page, int size) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(OrderService::toDto).getContent();
    }

    private static OrderDto toDto(CustomerOrder o) {
        List<OrderLine> lines = o.getItems().stream()
                .map(i -> new OrderLine(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity())).toList();
        return new OrderDto(o.getId(), o.getStatus(), o.getTotal(), o.getCreatedAt(), lines);
    }
}
