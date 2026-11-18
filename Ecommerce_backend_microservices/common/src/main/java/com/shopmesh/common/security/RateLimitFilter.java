package com.shopmesh.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis (INCR + EXPIRE), one counter per caller per minute.
 * Caller = user id when a valid JWT was present, otherwise the client IP (so login attempts are limited too).
 * If Redis is down the filter fails OPEN: the API stays up, only limiting is lost.
 */
@Component
@Order(20)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MS = 60_000;

    private final StringRedisTemplate redis;
    private final int limit;
    private final boolean enabled;
    private final String app;

    public RateLimitFilter(StringRedisTemplate redis,
                           @Value("${app.ratelimit.requests-per-minute:120}") int limit,
                           @Value("${app.ratelimit.enabled:true}") boolean enabled,
                           @Value("${spring.application.name:app}") String app) {
        this.redis = redis;
        this.limit = limit;
        this.enabled = enabled;
        this.app = app;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Object user = req.getAttribute(CurrentUser.ATTRIBUTE);
        String who = user instanceof CurrentUser u ? "u:" + u.id() : "ip:" + req.getRemoteAddr();
        long window = System.currentTimeMillis() / WINDOW_MS;
        String key = "rl:" + app + ":" + who + ":" + window;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(70));
            }
            if (count != null) {
                res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
                if (count > limit) {
                    long retryAfter = Math.max(1, (WINDOW_MS - System.currentTimeMillis() % WINDOW_MS) / 1000);
                    res.setHeader("Retry-After", String.valueOf(retryAfter));
                    HttpErrors.write(res, 429, "Too Many Requests", "rate limit exceeded");
                    return;
                }
            }
        } catch (RuntimeException e) {
            log.warn("rate limiter unavailable, failing open: {}", e.toString());
        }
        chain.doFilter(req, res);
    }
}
