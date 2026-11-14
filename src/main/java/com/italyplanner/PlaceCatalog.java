package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

@Service
@Transactional(readOnly = true)
public class PlaceCatalog {
    private static final Sort CATALOG_ORDER = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
    private final PlaceRepository repository;

    public PlaceCatalog(PlaceRepository repository) {
        this.repository = repository;
    }

    public List<JsonNode> search(String city, String region, String type, String tag, String query) {
        String cityFilter = normalize(city);
        String regionFilter = normalize(region);
        String typeFilter = normalize(type);
        String tagFilter = normalize(tag);
        String searchText = normalize(query);

        return repository.findAll(CATALOG_ORDER).stream()
                .map(Place::data)
                .filter(place -> matchesField(place, "city", cityFilter))
                .filter(place -> matchesField(place, "region", regionFilter))
                .filter(place -> matchesField(place, "type", typeFilter))
                .filter(place -> matchesTag(place, tagFilter))
                .filter(place -> searchText.isEmpty() || searchableText(place).contains(searchText))
                .toList();
    }

    public Optional<JsonNode> findById(String id) {
        return repository.findById(id).map(Place::data);
    }

    private static boolean matchesField(JsonNode place, String field, String filter) {
        return filter.isEmpty() || place.path(field).asText("").toLowerCase(Locale.ROOT).equals(filter);
    }

    private static boolean matchesTag(JsonNode place, String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        JsonNode tags = place.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                if (tag.asText().toLowerCase(Locale.ROOT).equals(filter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String searchableText(JsonNode place) {
        var fields = new StringJoiner(" ");
        for (String field : List.of("name", "city", "region", "neighborhood", "description")) {
            JsonNode value = place.path(field);
            if (value.isTextual()) {
                fields.add(value.textValue());
            }
        }
        return fields.toString().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
