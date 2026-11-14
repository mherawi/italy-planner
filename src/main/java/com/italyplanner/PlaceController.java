package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceCatalog catalog;
    private final PlaceImporter importer;

    public PlaceController(PlaceCatalog catalog, PlaceImporter importer) {
        this.catalog = catalog;
        this.importer = importer;
    }

    @GetMapping
    public List<JsonNode> search(
            @RequestParam(defaultValue = "") String city,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String tag,
            @RequestParam(name = "q", defaultValue = "") String query) {
        return catalog.search(city, region, type, tag, query);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> detail(@PathVariable String id) {
        return ResponseEntity.of(catalog.findById(id));
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importPlaces(@RequestBody JsonNode places) {
        try {
            return ResponseEntity.ok(Map.of("imported", importer.importPlaces(places)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage())).build();
        }
    }
}
