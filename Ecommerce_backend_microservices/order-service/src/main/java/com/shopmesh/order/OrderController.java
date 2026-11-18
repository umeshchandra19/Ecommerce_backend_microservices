package com.shopmesh.order;

import com.shopmesh.common.security.CurrentUser;
import com.shopmesh.order.Dtos.OrderDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                               HttpServletRequest http) {
        return service.list(CurrentUser.of(http).id(), Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable long id, HttpServletRequest http) {
        return service.get(CurrentUser.of(http).id(), id);
    }
}
