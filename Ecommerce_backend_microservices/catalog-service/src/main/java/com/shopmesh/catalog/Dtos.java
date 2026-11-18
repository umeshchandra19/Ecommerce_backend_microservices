package com.shopmesh.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    /** Serializable because these objects are stored in the Redis cache (JDK serialization). */
    public record ProductDto(Long id, String sku, String name, String description, BigDecimal price, int stock)
            implements Serializable {
        static ProductDto from(Product p) {
            return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(), p.getStock());
        }
    }

    public record ProductPage(List<ProductDto> items, int page, int size, long totalElements, int totalPages)
            implements Serializable {
    }

    public record ProductRequest(@NotBlank @Size(max = 64) String sku,
                                 @NotBlank @Size(max = 255) String name,
                                 @Size(max = 2000) String description,
                                 @NotNull @DecimalMin("0.01") BigDecimal price,
                                 @Min(0) int stock) {
    }

    public record StockItem(@NotNull Long productId, @Min(1) int quantity) {
    }

    public record StockRequest(@NotEmpty List<@Valid StockItem> items) {
    }

    /** Result of a successful reservation: name and price are snapshotted at reservation time. */
    public record ReservedLine(Long productId, String name, BigDecimal unitPrice, int quantity) {
    }
}
