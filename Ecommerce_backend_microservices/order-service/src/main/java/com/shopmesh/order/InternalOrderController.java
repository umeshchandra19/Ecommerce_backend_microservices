package com.shopmesh.order;

import com.shopmesh.common.security.CurrentUser;
import com.shopmesh.common.web.ApiException;
import com.shopmesh.order.Dtos.CreateOrderRequest;
import com.shopmesh.order.Dtos.OrderDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Orders are created only by checkout-service, which has just reserved stock and fetched trusted prices from
 * the catalog. Users therefore cannot POST their own prices (InternalKeyFilter guards /internal/**).
 */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {
    private final OrderService service;

    public InternalOrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest http) {
        return service.create(CurrentUser.of(http).id(), request);
    }

    @GetMapping("/by-key/{key}")
    public OrderDto byKey(@PathVariable String key, HttpServletRequest http) {
        return service.findByKey(CurrentUser.of(http).id(), key)
                .orElseThrow(() -> ApiException.notFound("no order for key"));
    }
}
