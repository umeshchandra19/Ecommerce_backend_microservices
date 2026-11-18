package com.shopmesh.cart;

import com.shopmesh.cart.Dtos.CartDto;
import com.shopmesh.cart.Dtos.CartLine;
import com.shopmesh.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private static final int MAX_QTY_PER_PRODUCT = 99;
    private final CartItemRepository repo;

    public CartService(CartItemRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public CartDto get(long userId) {
        return new CartDto(repo.findByUserIdOrderById(userId).stream()
                .map(i -> new CartLine(i.getProductId(), i.getQuantity())).toList());
    }

    @Transactional
    public CartDto add(long userId, long productId, int quantity) {
        CartItem item = repo.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> new CartItem(userId, productId, 0));
        int total = item.getQuantity() + quantity;
        if (total > MAX_QTY_PER_PRODUCT) {
            throw ApiException.badRequest("max " + MAX_QTY_PER_PRODUCT + " units per product");
        }
        item.setQuantity(total);
        repo.save(item);
        return get(userId);
    }

    @Transactional
    public CartDto remove(long userId, long productId) {
        repo.deleteByUserIdAndProductId(userId, productId);
        return get(userId);
    }

    @Transactional
    public void clear(long userId) {
        repo.deleteByUserId(userId);
    }
}
