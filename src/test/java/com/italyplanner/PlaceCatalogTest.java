package com.italyplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceCatalogTest {
    private final ObjectMapper json = new ObjectMapper();
    private final PlaceRepository repository = mock(PlaceRepository.class);
    private final PlaceCatalog catalog = new PlaceCatalog(repository);

    @Test
    void combinesFiltersAndMatchesSearchTextIgnoringCase() throws Exception {
        var match = json.readTree("""
                {"id":"a","name":"Gallery","city":"Florence","region":"Tuscany","type":"museum","tags":["art"]}
                """);
        var otherCity = json.readTree("""
                {"id":"b","name":"Gallery","city":"Rome","region":"Lazio","type":"museum","tags":["art"]}
                """);
        var otherTag = json.readTree("""
                {"id":"c","name":"Gallery","city":"Florence","region":"Tuscany","type":"museum","tags":["food"]}
                """);
        when(repository.findAll(any(Sort.class))).thenReturn(
                List.of(new Place(match), new Place(otherCity), new Place(otherTag)));

        assertThat(catalog.search(" Florence ", "TUSCANY", "MUSEUM", "ART", "gall"))
                .containsExactly(match);
    }

    @Test
    void returnsEmptyForUnknownId() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThat(catalog.findById("missing")).isEmpty();
    }
}
