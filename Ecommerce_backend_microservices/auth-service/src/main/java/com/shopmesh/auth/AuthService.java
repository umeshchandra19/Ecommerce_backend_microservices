package com.shopmesh.auth;

import com.shopmesh.auth.Dtos.*;
import com.shopmesh.common.security.JwtService;
import com.shopmesh.common.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "email already registered");
        }
        User u = users.save(new User(email, encoder.encode(r.password()), "USER"));
        return new UserResponse(u.getId(), u.getEmail(), u.getRole());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase(Locale.ROOT))
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("invalid credentials"));
        return new TokenResponse(jwt.generate(u.getId(), u.getEmail(), u.getRole()), "Bearer", jwt.getExpirySeconds());
    }
}
