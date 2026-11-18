package com.shopmesh.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    public record OrderLine(@NotNull Long productId,
                            @NotBlank String productName,
                            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
                            @Min(1) int quantity) {
    }

    public record CreateOrderRequest(@NotBlank @Size(max = 100) String idempotencyKey,
                                     @NotEmpty List<@Valid OrderLine> items) {
    }

    public record OrderDto(Long id, String status, BigDecimal total, Instant createdAt, List<OrderLine> items) {
    }
}
