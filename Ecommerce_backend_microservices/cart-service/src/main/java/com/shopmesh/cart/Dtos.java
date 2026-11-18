package com.shopmesh.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    public record AddItemRequest(@NotNull Long productId, @Min(1) @Max(99) int quantity) {
    }

    public record CartLine(Long productId, int quantity) {
    }

    public record CartDto(List<CartLine> items) {
    }
}
