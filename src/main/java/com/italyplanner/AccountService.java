package com.italyplanner;

import jakarta.persistence.EntityManager;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
public class AccountService {
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String dummyHash = passwords.encode("unused-password-placeholder");
    private final EntityManager entityManager;
    private final UserRepository users;
    private final AccountTokens tokens;

    public AccountService(EntityManager entityManager, UserRepository users, AccountTokens tokens) {
        this.entityManager = entityManager;
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional
    public SignIn register(String email, String password) {
        email = validate(email, password, true);
        entityManager.persist(new PlannerUser(email, passwords.encode(password)));
        return issueToken(email);
    }

    public SignIn signIn(String email, String password) {
        email = validate(email, password, false);
        var user = users.findById(email);
        String hash = user.map(PlannerUser::passwordHash).orElse(dummyHash);
        if (!passwords.matches(password, hash) || user.isEmpty()) {
            throw new AuthenticationFailed("Email or password is incorrect");
        }
        return issueToken(email);
    }

    public String currentEmail(String token) {
        if (token.isBlank()) {
            throw new AuthenticationFailed(null);
        }
        try {
            return tokens.email(token);
        } catch (JwtException invalid) {
            throw new AuthenticationFailed(null);
        }
    }

    private SignIn issueToken(String email) {
        return new SignIn(email, tokens.issue(email), AccountTokens.LIFETIME);
    }

    private static String validate(String email, String password, boolean registering) {
        String normalized = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !normalized.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") ||
                password == null || password.isEmpty() || password.getBytes(StandardCharsets.UTF_8).length > 72 ||
                (registering && password.length() < 8)) {
            throw new InvalidCredentials();
        }
        return normalized;
    }

    public record SignIn(String email, String token, Duration lifetime) {
    }

    static class InvalidCredentials extends RuntimeException {
        InvalidCredentials() {
            super("Invalid email or password");
        }
    }

    static class AuthenticationFailed extends RuntimeException {
        AuthenticationFailed(String message) {
            super(message);
        }
    }
}
