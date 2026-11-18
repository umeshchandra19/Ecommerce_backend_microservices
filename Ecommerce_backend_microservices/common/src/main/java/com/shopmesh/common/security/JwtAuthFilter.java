package com.shopmesh.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Requires a valid "Authorization: Bearer <jwt>" on every request except the configured public paths. */
@Component
@Order(10)
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final List<String> publicPaths;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthFilter(JwtService jwt,
                         @Value("${app.security.public-paths:/actuator/health,/actuator/health/**}") String publicPaths) {
        this.jwt = jwt;
        this.publicPaths = Arrays.stream(publicPaths.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return publicPaths.stream().anyMatch(p -> matcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            HttpErrors.write(res, 401, "Unauthorized", "missing bearer token");
            return;
        }
        try {
            req.setAttribute(CurrentUser.ATTRIBUTE, jwt.parse(header.substring(7).trim()));
        } catch (JwtException | IllegalArgumentException e) {
            HttpErrors.write(res, 401, "Unauthorized", "invalid or expired token");
            return;
        }
        chain.doFilter(req, res);
    }
}
