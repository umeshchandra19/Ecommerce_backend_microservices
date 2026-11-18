package com.shopmesh.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /internal/** endpoints are for service-to-service calls only. They need the shared X-Internal-Key header
 * (in addition to the user's JWT). In a real deployment these paths would also be blocked at the network edge.
 */
@Component
@Order(5)
public class InternalKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Internal-Key";
    private final byte[] expected;

    public InternalKeyFilter(@Value("${app.internal.key}") String key) {
        this.expected = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String given = req.getHeader(HEADER);
        if (given == null || !MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {
            HttpErrors.write(res, 403, "Forbidden", "internal endpoint");
            return;
        }
        chain.doFilter(req, res);
    }
}
