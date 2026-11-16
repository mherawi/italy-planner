package com.italyplanner;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTokensTest {
    @Test
    void readsTokensAcrossInstancesWithTheSameConfiguredKey() {
        String secret = "test-only-key-with-at-least-32-bytes";
        String token = new AccountTokens(secret).issue("traveler@example.com");

        assertThat(new AccountTokens(secret).email(token)).isEqualTo("traveler@example.com");
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        String token = new AccountTokens("").issue("traveler@example.com");

        assertThatThrownBy(() -> new AccountTokens("").email(token))
                .isInstanceOf(JwtException.class);
    }
}
