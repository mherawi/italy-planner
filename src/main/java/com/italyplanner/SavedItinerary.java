package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "planner_itineraries")
class SavedItinerary {
    @Id
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode plan;

    protected SavedItinerary() {
    }

    SavedItinerary(String email, JsonNode plan) {
        this.email = email;
        this.plan = plan;
    }

    JsonNode plan() {
        return plan;
    }

    void replace(JsonNode plan) {
        this.plan = plan;
    }
}
