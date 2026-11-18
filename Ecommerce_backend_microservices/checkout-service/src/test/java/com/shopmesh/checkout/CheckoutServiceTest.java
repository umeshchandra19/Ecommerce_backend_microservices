package com.shopmesh.checkout;

import com.shopmesh.checkout.Dtos.*;
import com.shopmesh.common.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutServiceTest {
    private static final String AUTH = "Bearer t";
    private static final String KEY = "k-1";

    private CartClient carts;
    private CatalogClient catalog;
    private OrderClient orders;
    private CheckoutService service;
    private final OrderDto order = new OrderDto(10L, "CREATED", new BigDecimal("5.00"), Instant.now(),
            List.of(new OrderLine(1L, "Pen", new BigDecimal("2.50"), 2)));

    @BeforeEach
    void setUp() {
        carts = mock(CartClient.class);
        catalog = mock(CatalogClient.class);
        orders = mock(OrderClient.class);
        service = new CheckoutService(carts, catalog, orders);
        when(carts.get(AUTH)).thenReturn(new CartDto(List.of(new CartLine(1L, 2))));
        when(catalog.reserve(eq(AUTH), anyList())).thenReturn(List.of(new ReservedLine(1L, "Pen", new BigDecimal("2.50"), 2)));
    }

    @Test
    void happyPathCreatesOrderAndClearsCart() {
        when(orders.findByKey(AUTH, KEY)).thenReturn(Optional.empty());
        when(orders.create(eq(AUTH), any())).thenReturn(order);

        assertEquals(10L, service.checkout(AUTH, KEY).id());
        verify(carts).clear(AUTH);
        verify(catalog, never()).release(any(), anyList());
    }

    @Test
    void releasesStockWhenOrderCreationFails() {
        when(orders.findByKey(AUTH, KEY)).thenReturn(Optional.empty());
        when(orders.create(eq(AUTH), any())).thenThrow(new RuntimeException("order-service down"));

        assertThrows(RuntimeException.class, () -> service.checkout(AUTH, KEY));
        verify(catalog).release(eq(AUTH), anyList());
        verify(carts, never()).clear(any());
    }

    @Test
    void keepsStockWhenOrderWasSavedButResponseWasLost() {
        when(orders.findByKey(AUTH, KEY)).thenReturn(Optional.empty(), Optional.of(order));
        when(orders.create(eq(AUTH), any())).thenThrow(new RuntimeException("read timeout"));

        assertEquals(10L, service.checkout(AUTH, KEY).id());
        verify(catalog, never()).release(any(), anyList());
    }

    @Test
    void replayWithSameKeyDoesNotReserveAgain() {
        when(orders.findByKey(AUTH, KEY)).thenReturn(Optional.of(order));

        assertEquals(10L, service.checkout(AUTH, KEY).id());
        verify(catalog, never()).reserve(any(), anyList());
    }

    @Test
    void emptyCartIsRejected() {
        when(orders.findByKey(AUTH, KEY)).thenReturn(Optional.empty());
        when(carts.get(AUTH)).thenReturn(new CartDto(List.of()));

        assertThrows(ApiException.class, () -> service.checkout(AUTH, KEY));
        verify(catalog, never()).reserve(any(), anyList());
    }
}
