package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Optional;

@Service
class ItineraryService {
    private final EntityManager entities;
    private final ObjectMapper json;
    private final AccountService accounts;

    ItineraryService(EntityManager entities, ObjectMapper json, AccountService accounts) {
        this.entities = entities;
        this.json = json;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Optional<JsonNode> load(String token, String expectedAccount) {
        String email = authenticatedEmail(token, expectedAccount);
        return Optional.ofNullable(entities.find(SavedItinerary.class, email)).map(SavedItinerary::plan);
    }

    @Transactional
    public void save(String token, String expectedAccount, JsonNode body) {
        String email = authenticatedEmail(token, expectedAccount);
        JsonNode plan = validatedPlan(body);
        // Lock the account row so two first saves cannot both insert the itinerary.
        if (entities.find(PlannerUser.class, email, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new AccountService.AuthenticationFailed(null);
        }
        var saved = entities.find(SavedItinerary.class, email);
        if (saved == null) {
            entities.persist(new SavedItinerary(email, plan));
        } else {
            saved.replace(plan);
        }
    }

    private String authenticatedEmail(String token, String expectedAccount) {
        String email = accounts.currentEmail(token);
        try {
            if (email.equals(URLDecoder.decode(expectedAccount, StandardCharsets.UTF_8))) {
                return email;
            }
        } catch (IllegalArgumentException invalidEncoding) {
            throw new AccountService.AuthenticationFailed(null);
        }
        throw new AccountService.AuthenticationFailed(null);
    }

    private JsonNode validatedPlan(JsonNode body) {
        JsonNode days = body.path("days");
        JsonNode startDate = body.path("startDate");
        if (!days.isArray() || days.isEmpty() || !startDate.isTextual()) {
            throw new InvalidItinerary();
        }
        String date = startDate.textValue();
        if (!date.isEmpty()) {
            try {
                if (!LocalDate.parse(date).toString().equals(date) || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                        || date.startsWith("0000")) {
                    throw new InvalidItinerary();
                }
            } catch (DateTimeParseException invalid) {
                throw new InvalidItinerary();
            }
        }
        for (JsonNode day : days) {
            if (!day.isArray()) {
                throw new InvalidItinerary();
            }
            var ids = new HashSet<String>();
            for (JsonNode id : day) {
                if (!id.isTextual() || id.textValue().isBlank() || id.textValue().indexOf(0) >= 0 ||
                        !ids.add(id.textValue())) {
                    throw new InvalidItinerary();
                }
            }
        }
        var plan = json.createObjectNode();
        plan.set("days", days);
        plan.put("startDate", date);
        return plan;
    }

    static class InvalidItinerary extends RuntimeException {
    }
}
