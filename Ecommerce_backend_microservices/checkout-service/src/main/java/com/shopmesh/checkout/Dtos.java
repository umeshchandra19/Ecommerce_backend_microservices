package com.shopmesh.checkout;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Local copies of the other services' API shapes (no shared model library, so services can deploy independently). */
public final class Dtos {
    private Dtos() {
    }

    public record CartLine(Long productId, int quantity) {
    }

    public record CartDto(List<CartLine> items) {
    }

    public record StockItem(Long productId, int quantity) {
    }

    public record StockRequest(List<StockItem> items) {
    }

    public record ReservedLine(Long productId, String name, BigDecimal unitPrice, int quantity) {
    }

    public record OrderLine(Long productId, String productName, BigDecimal unitPrice, int quantity) {
    }

    public record CreateOrderRequest(String idempotencyKey, List<OrderLine> items) {
    }

    public record OrderDto(Long id, String status, BigDecimal total, Instant createdAt, List<OrderLine> items) {
    }
}
