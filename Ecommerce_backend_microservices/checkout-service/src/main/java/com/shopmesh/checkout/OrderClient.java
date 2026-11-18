package com.shopmesh.checkout;

import com.shopmesh.checkout.Dtos.CreateOrderRequest;
import com.shopmesh.checkout.Dtos.OrderDto;
import com.shopmesh.common.security.InternalKeyFilter;
import com.shopmesh.common.web.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class OrderClient {
    private final RestClient http;
    private final String internalKey;

    public OrderClient(@Qualifier("orderHttp") RestClient http, @Value("${app.internal.key}") String internalKey) {
        this.http = http;
        this.internalKey = internalKey;
    }

    /** Safe to call again with the same key: order-service returns the existing order. */
    @CircuitBreaker(name = "order")
    public OrderDto create(String auth, CreateOrderRequest request) {
        return Downstream.mapClientErrors(
                http.post().uri("/internal/orders")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header(InternalKeyFilter.HEADER, internalKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request).retrieve(), "order-service")
                .body(OrderDto.class);
    }

    @CircuitBreaker(name = "order")
    @Retry(name = "read")
    public Optional<OrderDto> findByKey(String auth, String key) {
        try {
            return Optional.ofNullable(Downstream.mapClientErrors(
                    http.get().uri("/internal/orders/by-key/{key}", key)
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .header(InternalKeyFilter.HEADER, internalKey).retrieve(), "order-service")
                    .body(OrderDto.class));
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
