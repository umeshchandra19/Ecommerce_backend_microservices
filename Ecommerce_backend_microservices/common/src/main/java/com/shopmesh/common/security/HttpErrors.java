package com.shopmesh.common.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Writes the same JSON error shape as GlobalExceptionHandler from inside servlet filters. */
final class HttpErrors {
    private HttpErrors() {
    }

    static void write(HttpServletResponse res, int status, String error, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
