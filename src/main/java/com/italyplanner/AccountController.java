package com.italyplanner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    static final String USER_EMAIL = "accountEmail";
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public Profile current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(USER_EMAIL) instanceof String email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return new Profile(email);
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    @ResponseStatus(HttpStatus.CREATED)
    public Profile register(@RequestBody Credentials body, HttpServletRequest request) {
        String email = validate(body, true);
        try {
            accounts.register(email, body.password());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return signIn(email, request);
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    public Profile login(@RequestBody Credentials body, HttpServletRequest request) {
        String email = validate(body, false);
        if (!accounts.authenticate(email, body.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }
        return signIn(email, request);
    }

    @PostMapping(path = "/logout", headers = "X-Planner-Request=account")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private static Profile signIn(String email, HttpServletRequest request) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        request.getSession().setAttribute(USER_EMAIL, email);
        return new Profile(email);
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
