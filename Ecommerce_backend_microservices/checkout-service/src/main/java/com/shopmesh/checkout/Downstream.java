package com.shopmesh.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmesh.common.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * 4xx answers from a downstream service are the caller's business errors (e.g. 409 "insufficient stock"):
 * they are passed on as ApiException and do NOT count against the circuit breaker.
 * 5xx and timeouts stay as RestClientException and DO count.
 */
final class Downstream {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Downstream() {
    }

    static RestClient.ResponseSpec mapClientErrors(RestClient.ResponseSpec spec, String service) {
        return spec.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
            throw new ApiException(status != null ? status : HttpStatus.BAD_REQUEST, service + ": " + messageOf(body));
        });
    }

    private static String messageOf(String body) {
        try {
            String m = JSON.readTree(body).path("message").asText("");
            return m.isEmpty() ? truncate(body) : m;
        } catch (Exception e) {
            return truncate(body);
        }
    }

    private static String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
