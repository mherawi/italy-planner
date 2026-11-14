package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;

@Service
public class PlaceImporter {
    private final PlaceRepository repository;
    private final EntityManager entityManager;

    public PlaceImporter(PlaceRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional
    public int importPlaces(JsonNode places) {
        return writePlaces(places, true);
    }

    @Transactional
    public void seedPlaces(JsonNode places) {
        writePlaces(places, false);
    }

    private int writePlaces(JsonNode places, boolean replaceExisting) {
        if (places == null || !places.isArray() || places.isEmpty()) {
            throw new IllegalArgumentException("Expected a nonempty JSON array of places");
        }

        var byId = new LinkedHashMap<String, JsonNode>();
        for (int index = 0; index < places.size(); index++) {
            JsonNode place = places.get(index);
            try {
                validate(place);
                String id = place.get("id").textValue();
                if (byId.putIfAbsent(id, place) != null) {
                    throw new IllegalArgumentException("Duplicate ID " + id);
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Place " + (index + 1) + ". " + exception.getMessage());
            }
        }

        // Serialize catalog writes so overlapping imports can safely insert the same new ID.
        var lock = entityManager.find(PlaceImportLock.class, PlaceImportLock.CATALOG_ID, LockModeType.PESSIMISTIC_WRITE);
        if (lock == null) {
            throw new IllegalStateException("Place import lock is missing");
        }
        var existing = new HashMap<String, Place>();
        for (Place place : repository.findAllById(byId.keySet())) {
            existing.put(place.id(), place);
        }
        for (var entry : byId.entrySet()) {
            Place place = existing.get(entry.getKey());
            if (place == null) {
                entityManager.persist(new Place(entry.getValue()));
            } else if (replaceExisting) {
                place.replaceData(entry.getValue());
            }
        }
        return byId.size();
    }

    private static void validate(JsonNode place) {
        for (String field : new String[]{"id", "name", "type", "city"}) {
            JsonNode value = place.path(field);
            if (!value.isTextual() || value.textValue().isBlank() || value.textValue().indexOf(0) >= 0) {
                throw new IllegalArgumentException(field + " must be a nonblank string");
            }
        }
        for (String field : new String[]{"region", "neighborhood", "description", "hours", "price_range", "seasonal_notes"}) {
            JsonNode value = place.get(field);
            if (value != null && !value.isNull() && !value.isTextual()) {
                throw new IllegalArgumentException(field + " must be a string or null");
            }
        }
        for (String field : new String[]{"latitude", "longitude", "rating"}) {
            JsonNode value = place.get(field);
            if (value != null && !value.isNull() && (!value.isNumber() || !Double.isFinite(value.doubleValue()))) {
                throw new IllegalArgumentException(field + " must be a finite number or null");
            }
        }
        JsonNode duration = place.get("duration_minutes");
        if (duration != null && !duration.isNull() &&
                (!duration.isIntegralNumber() || !duration.canConvertToInt() || duration.intValue() < 0)) {
            throw new IllegalArgumentException("duration_minutes must be a nonnegative integer or null");
        }
        JsonNode booking = place.get("booking_required");
        if (booking != null && !booking.isNull() && !booking.isBoolean()) {
            throw new IllegalArgumentException("booking_required must be a boolean or null");
        }
        JsonNode tags = place.get("tags");
        if (tags == null || tags.isNull()) {
            return;
        }
        if (!tags.isArray()) {
            throw new IllegalArgumentException("tags must be an array of strings or null");
        }
        for (JsonNode tag : tags) {
            if (!tag.isTextual()) {
                throw new IllegalArgumentException("tags must contain only strings");
            }
        }
    }
}
