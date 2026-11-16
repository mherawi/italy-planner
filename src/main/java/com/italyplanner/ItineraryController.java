package com.italyplanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account/itinerary")
class ItineraryController {
    private final ItineraryService itineraries;

    ItineraryController(ItineraryService itineraries) {
        this.itineraries = itineraries;
    }

    @GetMapping
    public ResponseEntity<JsonNode> load(@CookieValue(name = AccountController.COOKIE, defaultValue = "") String token,
                                         @RequestHeader(name = "X-Planner-Account", defaultValue = "") String account) {
        return itineraries.load(token, account)
                .map(plan -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(plan))
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, headers = "X-Planner-Request=account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@CookieValue(name = AccountController.COOKIE, defaultValue = "") String token,
                     @RequestHeader(name = "X-Planner-Account", defaultValue = "") String account,
                     @RequestBody JsonNode body) {
        itineraries.save(token, account, body);
    }

    @ExceptionHandler(AccountService.AuthenticationFailed.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void unauthorized() {
    }

    @ExceptionHandler(ItineraryService.InvalidItinerary.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidItinerary() {
    }
}
