package com.shopmesh.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RateLimitFilterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    RateLimitFilterTest() {
        when(redis.opsForValue()).thenReturn(ops);
    }

    private int call(RateLimitFilter filter, MockFilterChain chain) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/x"), res, chain);
        return res.getStatus();
    }

    @Test
    void blocksAfterLimit() throws Exception {
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);
        RateLimitFilter filter = new RateLimitFilter(redis, 2, true, "test");

        assertEquals(200, call(filter, new MockFilterChain()));
        assertEquals(200, call(filter, new MockFilterChain()));
        MockFilterChain third = new MockFilterChain();
        assertEquals(429, call(filter, third));
        assertNull(third.getRequest(), "request must not reach the controller when limited");
    }

    @Test
    void failsOpenWhenRedisIsDown() throws Exception {
        when(ops.increment(anyString())).thenThrow(new RuntimeException("redis down"));
        RateLimitFilter filter = new RateLimitFilter(redis, 1, true, "test");
        MockFilterChain chain = new MockFilterChain();
        assertEquals(200, call(filter, chain));
        assertNotNull(chain.getRequest());
    }
}
