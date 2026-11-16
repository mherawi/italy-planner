package com.italyplanner;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private static final String COOKIE = "planner_account";
    private final AccountService accounts;
    private final AccountTokens tokens;
    private final boolean secureCookie;

    public AccountController(AccountService accounts, AccountTokens tokens,
                             @Value("${planner.jwt.cookie-secure:${server.servlet.session.cookie.secure:false}}") boolean secureCookie) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.secureCookie = secureCookie;
    }

    @GetMapping
    public Profile current(@CookieValue(name = COOKIE, defaultValue = "") String token) {
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return new Profile(tokens.email(token));
        } catch (JwtException invalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    @ResponseStatus(HttpStatus.CREATED)
    public Profile register(@RequestBody Credentials body, HttpServletResponse response) {
        String email = validate(body, true);
        try {
            accounts.register(email, body.password());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return signIn(email, response);
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    public Profile login(@RequestBody Credentials body, HttpServletResponse response) {
        String email = validate(body, false);
        if (!accounts.authenticate(email, body.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }
        return signIn(email, response);
    }

    @PostMapping(path = "/logout", headers = "X-Planner-Request=account")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private Profile signIn(String email, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.issue(email), AccountTokens.LIFETIME));
        return new Profile(email);
    }

    private String cookie(String value, Duration age) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(secureCookie)
                .sameSite("Lax").path("/api/account").maxAge(age).build().toString();
    }

    private static String validate(Credentials body, boolean registering) {
        String email = body.email() == null ? "" : body.email().strip().toLowerCase(Locale.ROOT);
        String password = body.password();
        if (email.length() > 254 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") ||
                password == null || password.isEmpty() || password.getBytes(StandardCharsets.UTF_8).length > 72 ||
                (registering && password.length() < 8)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or password");
        }
        return email;
    }

    public record Credentials(String email, String password) {
    }

    public record Profile(String email) {
    }
}
