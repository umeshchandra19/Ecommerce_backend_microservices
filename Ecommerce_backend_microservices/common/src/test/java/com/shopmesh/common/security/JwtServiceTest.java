package com.shopmesh.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER = "ffffffffffffffffeeeeeeeeeeeeeeee";

    @Test
    void roundTripKeepsClaims() {
        JwtService s = new JwtService(SECRET, 60);
        CurrentUser u = s.parse(s.generate(7, "a@b.com", "USER"));
        assertEquals(7, u.id());
        assertEquals("a@b.com", u.email());
        assertEquals("USER", u.role());
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = new JwtService(OTHER, 60).generate(1, "x@y.com", "USER");
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60).parse(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService s = new JwtService(SECRET, -10);
        String token = s.generate(1, "x@y.com", "USER");
        assertThrows(JwtException.class, () -> s.parse(token));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60));
    }
}
