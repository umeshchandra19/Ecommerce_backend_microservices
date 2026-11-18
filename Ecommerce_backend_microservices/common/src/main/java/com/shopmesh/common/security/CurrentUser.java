package com.shopmesh.common.security;

import com.shopmesh.common.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;

/** The authenticated caller, extracted from the JWT by {@link JwtAuthFilter}. */
public record CurrentUser(long id, String email, String role) {
    public static final String ATTRIBUTE = "shopmesh.currentUser";

    public static CurrentUser of(HttpServletRequest request) {
        Object o = request.getAttribute(ATTRIBUTE);
        if (o instanceof CurrentUser u) {
            return u;
        }
        throw ApiException.unauthorized("authentication required");
    }

    public void requireRole(String required) {
        if (!required.equals(role)) {
            throw ApiException.forbidden("requires role " + required);
        }
    }
}
