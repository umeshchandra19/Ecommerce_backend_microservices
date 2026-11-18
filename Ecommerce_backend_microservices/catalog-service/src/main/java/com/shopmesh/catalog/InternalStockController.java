package com.shopmesh.catalog;

import com.shopmesh.catalog.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Service-to-service API for checkout-service (protected by InternalKeyFilter). */
@RestController
@RequestMapping("/internal/stock")
public class InternalStockController {
    private final ProductService service;

    public InternalStockController(ProductService service) {
        this.service = service;
    }

    @PostMapping("/reserve")
    public List<ReservedLine> reserve(@Valid @RequestBody StockRequest request) {
        return service.reserve(request.items());
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody StockRequest request) {
        service.release(request.items());
    }
}
