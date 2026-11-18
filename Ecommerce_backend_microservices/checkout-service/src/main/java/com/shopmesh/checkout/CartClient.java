package com.shopmesh.checkout;

import com.shopmesh.checkout.Dtos.CartDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CartClient {
    private final RestClient http;

    public CartClient(@Qualifier("cartHttp") RestClient http) {
        this.http = http;
    }

    @CircuitBreaker(name = "cart")
    @Retry(name = "read")
    public CartDto get(String auth) {
        return Downstream.mapClientErrors(
                http.get().uri("/api/cart").header(HttpHeaders.AUTHORIZATION, auth).retrieve(), "cart-service")
                .body(CartDto.class);
    }

    @CircuitBreaker(name = "cart")
    public void clear(String auth) {
        Downstream.mapClientErrors(
                http.delete().uri("/api/cart").header(HttpHeaders.AUTHORIZATION, auth).retrieve(), "cart-service")
                .toBodilessEntity();
    }
}
