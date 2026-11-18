package com.shopmesh.checkout;

import com.shopmesh.checkout.Dtos.OrderDto;
import com.shopmesh.common.security.CurrentUser;
import com.shopmesh.common.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    /** Send a unique Idempotency-Key per purchase attempt; retrying with the same key never double-charges stock. */
    @PostMapping
    public ResponseEntity<OrderDto> checkout(@RequestHeader("Idempotency-Key") String idempotencyKey, HttpServletRequest http) {
        CurrentUser.of(http); // 401 if not authenticated
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw ApiException.badRequest("Idempotency-Key must be 1-100 characters");
        }
        OrderDto order = service.checkout(http.getHeader(HttpHeaders.AUTHORIZATION), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
