package com.italyplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PlaceSeedRunner implements ApplicationRunner {
    private final ObjectMapper json;
    private final PlaceImporter importer;

    public PlaceSeedRunner(ObjectMapper json, PlaceImporter importer) {
        this.json = json;
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try (var input = new ClassPathResource("italy.json").getInputStream()) {
            importer.seedPlaces(json.readTree(input));
        }
    }
}
