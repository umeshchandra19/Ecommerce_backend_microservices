package com.shopmesh.cart;

import com.shopmesh.cart.Dtos.AddItemRequest;
import com.shopmesh.cart.Dtos.CartDto;
import com.shopmesh.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartDto get(HttpServletRequest http) {
        return service.get(CurrentUser.of(http).id());
    }

    @PostMapping("/items")
    public CartDto add(@Valid @RequestBody AddItemRequest request, HttpServletRequest http) {
        return service.add(CurrentUser.of(http).id(), request.productId(), request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartDto remove(@PathVariable long productId, HttpServletRequest http) {
        return service.remove(CurrentUser.of(http).id(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(HttpServletRequest http) {
        service.clear(CurrentUser.of(http).id());
    }
}
