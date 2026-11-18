package com.shopmesh.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InternalKeyFilterTest {
    private final InternalKeyFilter filter = new InternalKeyFilter("secret-key");

    @Test
    void rejectsInternalCallWithoutKey() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/internal/orders"), res, new MockFilterChain());
        assertEquals(403, res.getStatus());
    }

    @Test
    void allowsInternalCallWithKey() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/internal/orders");
        req.addHeader(InternalKeyFilter.HEADER, "secret-key");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
    }

    @Test
    void ignoresPublicApiPaths() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/products"), new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
    }
}
