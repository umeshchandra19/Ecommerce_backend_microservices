package com.shopmesh.checkout;

import com.shopmesh.checkout.Dtos.*;
import com.shopmesh.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the checkout as a small saga:
 * <ol>
 *   <li>replay check: same Idempotency-Key => return the order that already exists</li>
 *   <li>read the cart</li>
 *   <li>reserve stock in catalog (all-or-nothing, also returns trusted prices)</li>
 *   <li>create the order; if that fails, compensate by releasing the stock</li>
 *   <li>clear the cart (best effort: the order already exists)</li>
 * </ol>
 */
@Service
public class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartClient carts;
    private final CatalogClient catalog;
    private final OrderClient orders;

    public CheckoutService(CartClient carts, CatalogClient catalog, OrderClient orders) {
        this.carts = carts;
        this.catalog = catalog;
        this.orders = orders;
    }

    public OrderDto checkout(String auth, String idempotencyKey) {
        Optional<OrderDto> replay = orders.findByKey(auth, idempotencyKey);
        if (replay.isPresent()) {
            clearCartQuietly(auth);
            return replay.get();
        }

        CartDto cart = carts.get(auth);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw ApiException.badRequest("cart is empty");
        }
        List<StockItem> items = cart.items().stream().map(l -> new StockItem(l.productId(), l.quantity())).toList();

        List<ReservedLine> reserved = catalog.reserve(auth, items);
        List<OrderLine> lines = reserved.stream()
                .map(r -> new OrderLine(r.productId(), r.name(), r.unitPrice(), r.quantity())).toList();

        OrderDto order;
        try {
            order = orders.create(auth, new CreateOrderRequest(idempotencyKey, lines));
        } catch (RuntimeException failure) {
            // The call may have failed AFTER the order was saved (e.g. response timeout). Check before releasing stock.
            Optional<OrderDto> saved = findQuietly(auth, idempotencyKey);
            if (saved.isPresent()) {
                clearCartQuietly(auth);
                return saved.get();
            }
            releaseQuietly(auth, items);
            throw failure;
        }
        clearCartQuietly(auth);
        return order;
    }

    private Optional<OrderDto> findQuietly(String auth, String key) {
        try {
            return orders.findByKey(auth, key);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private void releaseQuietly(String auth, List<StockItem> items) {
        try {
            catalog.release(auth, items);
        } catch (RuntimeException e) {
            // Needs manual/automated reconciliation (an outbox or retry job would fix this; see README roadmap).
            log.error("COMPENSATION FAILED: stock for {} was reserved but could not be released", items, e);
        }
    }

    private void clearCartQuietly(String auth) {
        try {
            carts.clear(auth);
        } catch (RuntimeException e) {
            log.warn("order created but cart could not be cleared: {}", e.toString());
        }
    }
}
