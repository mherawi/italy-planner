package com.italyplanner;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    static final String COOKIE = "planner_account";
    private final AccountService accounts;
    private final boolean secureCookie;

    public AccountController(AccountService accounts,
                             @Value("${planner.jwt.cookie-secure:${server.servlet.session.cookie.secure:false}}") boolean secureCookie) {
        this.accounts = accounts;
        this.secureCookie = secureCookie;
    }

    @GetMapping
    public Profile current(@CookieValue(name = COOKIE, defaultValue = "") String token) {
        try {
            return new Profile(accounts.currentEmail(token));
        } catch (AccountService.AuthenticationFailed invalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    @ResponseStatus(HttpStatus.CREATED)
    public Profile register(@RequestBody Credentials body, HttpServletResponse response) {
        try {
            return writeSignInCookie(accounts.register(body.email(), body.password()), response);
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    public Profile login(@RequestBody Credentials body, HttpServletResponse response) {
        try {
            return writeSignInCookie(accounts.signIn(body.email(), body.password()), response);
        } catch (AccountService.AuthenticationFailed invalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, invalid.getMessage());
        }
    }

    @PostMapping(path = "/logout", headers = "X-Planner-Request=account")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private Profile writeSignInCookie(AccountService.SignIn signIn, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(signIn.token(), signIn.lifetime()));
        return new Profile(signIn.email());
    }

    private String cookie(String value, Duration age) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(secureCookie)
                .sameSite("Lax").path("/api/account").maxAge(age).build().toString();
    }

    @ExceptionHandler(AccountService.InvalidCredentials.class)
    public void invalidCredentials(AccountService.InvalidCredentials error, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), error.getMessage());
    }

    public record Credentials(String email, String password) {
    }

    public record Profile(String email) {
    }
}
