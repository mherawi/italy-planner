package com.italyplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItineraryServiceTest {
    @Test
    void rejectsAnItineraryWriteIfTheBrowserHasSwitchedAccounts() throws Exception {
        var entities = mock(EntityManager.class);
        var accounts = mock(AccountService.class);
        var json = new ObjectMapper();
        var itineraries = new ItineraryService(entities, json, accounts);
        when(accounts.currentEmail("new-account-token")).thenReturn("second@example.com");
        var plan = json.readTree("""
                {"days":[["museum"]],"startDate":"2027-05-01"}
                """);

        assertThatThrownBy(() -> itineraries.save("new-account-token", "first%40example.com", plan))
                .isInstanceOf(AccountService.AuthenticationFailed.class);
        verifyNoInteractions(entities);
    }
}
