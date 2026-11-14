package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.JdkClientHttpRequestFactory;

@Service
public class WalkingRoutes {
    private final PlaceCatalog catalog;
    private final RestClient client;
    private final String apiKey;

    public WalkingRoutes(PlaceCatalog catalog, @Value("${OPENROUTESERVICE_API_KEY:}") String apiKey) {
        this.catalog = catalog;
        this.apiKey = apiKey;
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(4));
        client = RestClient.builder()
                .baseUrl("https://api.heigit.org")
                .requestFactory(factory)
                .build();
    }

    public Estimate estimate(String fromId, String toId) {
        JsonNode from = place(fromId);
        JsonNode to = place(toId);
        if (!from.path("city").asText().equalsIgnoreCase(to.path("city").asText())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Check intercity travel");
        }

        Coordinates start = Coordinates.from(from);
        Coordinates end = Coordinates.from(to);
        double distance = start.distanceTo(end);
        if (distance == 0) {
            return new Estimate(0, "same_location");
        }

        if (!apiKey.isBlank()) {
            try {
                JsonNode response = client.post().uri("/openrouteservice/v2/directions/foot-walking")
                        .header("Authorization", apiKey)
                        .body(Map.of("coordinates", List.of(start.asPair(), end.asPair())))
                        .retrieve().body(JsonNode.class);
                if (response != null) {
                    JsonNode seconds = response.path("routes").path(0).path("summary").path("duration");
                    if (seconds.isNumber() && seconds.asDouble() > 0 && Double.isFinite(seconds.asDouble())) {
                        return new Estimate((int) Math.ceil(seconds.asDouble() / 60), "openrouteservice");
                    }
                }
            } catch (RestClientException ignored) {
                // Keep a rough estimate available if the routing provider cannot return a route.
            }
        }

        return new Estimate((int) Math.ceil(distance / 4.5 * 60), "straight_line");
    }

    private JsonNode place(String id) {
        return catalog.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown place"));
    }

    public record Estimate(int minutes, String source) {
    }

    private record Coordinates(double latitude, double longitude) {
        static Coordinates from(JsonNode place) {
            double lat = place.path("latitude").asDouble(Double.NaN);
            double lon = place.path("longitude").asDouble(Double.NaN);
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coordinates unavailable");
            }
            return new Coordinates(lat, lon);
        }

        List<Double> asPair() {
            return List.of(longitude, latitude);
        }

        double distanceTo(Coordinates other) {
            double latDelta = Math.toRadians(other.latitude - latitude);
            double lonDelta = Math.toRadians(other.longitude - longitude);
            double latTerm = Math.sin(latDelta / 2);
            double lonTerm = Math.sin(lonDelta / 2);
            double haversine = latTerm * latTerm + Math.cos(Math.toRadians(latitude))
                    * Math.cos(Math.toRadians(other.latitude)) * lonTerm * lonTerm;
            return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, haversine)));
        }
    }
}
