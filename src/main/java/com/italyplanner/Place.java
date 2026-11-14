package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "places")
public class Place {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String city;

    private String region;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode data;

    protected Place() {
    }

    Place(JsonNode data) {
        id = data.get("id").textValue();
        replaceData(data);
    }

    String id() {
        return id;
    }

    JsonNode data() {
        return data;
    }

    void replaceData(JsonNode data) {
        name = data.get("name").textValue();
        type = data.get("type").textValue();
        city = data.get("city").textValue();
        region = data.path("region").textValue();
        this.data = data;
    }
}
