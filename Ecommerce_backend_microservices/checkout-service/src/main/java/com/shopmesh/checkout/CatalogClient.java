package com.shopmesh.checkout;

import com.shopmesh.checkout.Dtos.ReservedLine;
import com.shopmesh.checkout.Dtos.StockItem;
import com.shopmesh.checkout.Dtos.StockRequest;
import com.shopmesh.common.security.InternalKeyFilter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** No automatic retry on reserve/release: they change stock and are not idempotent. */
@Component
public class CatalogClient {
    private final RestClient http;
    private final String internalKey;

    public CatalogClient(@Qualifier("catalogHttp") RestClient http, @Value("${app.internal.key}") String internalKey) {
        this.http = http;
        this.internalKey = internalKey;
    }

    @CircuitBreaker(name = "catalog")
    public List<ReservedLine> reserve(String auth, List<StockItem> items) {
        ReservedLine[] lines = Downstream.mapClientErrors(
                http.post().uri("/internal/stock/reserve")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header(InternalKeyFilter.HEADER, internalKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new StockRequest(items)).retrieve(), "catalog-service")
                .body(ReservedLine[].class);
        return lines == null ? List.of() : List.of(lines);
    }

    @CircuitBreaker(name = "catalog")
    public void release(String auth, List<StockItem> items) {
        Downstream.mapClientErrors(
                http.post().uri("/internal/stock/release")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header(InternalKeyFilter.HEADER, internalKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new StockRequest(items)).retrieve(), "catalog-service")
                .toBodilessEntity();
    }
}
