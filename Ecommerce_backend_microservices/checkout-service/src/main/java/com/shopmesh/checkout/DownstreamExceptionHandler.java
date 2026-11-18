package com.shopmesh.checkout;

import com.shopmesh.common.web.ApiError;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

/** A broken or slow downstream service becomes a clean 503 instead of a 500 stack trace. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DownstreamExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DownstreamExceptionHandler.class);

    @ExceptionHandler({CallNotPermittedException.class, RestClientException.class})
    public ResponseEntity<ApiError> unavailable(Exception e) {
        log.warn("downstream unavailable: {}", e.toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(Instant.now(), 503,
                "Service Unavailable", "a dependent service is temporarily unavailable, please retry"));
    }
}
