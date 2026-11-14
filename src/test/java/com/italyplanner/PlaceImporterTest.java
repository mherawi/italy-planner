package com.italyplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceImporterTest {
    private final ObjectMapper json = new ObjectMapper();
    private final PlaceRepository repository = mock(PlaceRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final PlaceImporter importer = new PlaceImporter(repository, entityManager);

    @Test
    void rejectsMissingRequiredFieldsBeforeWriting() throws Exception {
        var places = json.readTree("""
                [{"id":"a","name":"Gallery","type":"museum","city":"Florence"},{"id":"b","name":"Market"}]
                """);

        assertThatThrownBy(() -> importer.importPlaces(places))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Place 2. type must be a nonblank string");
        verifyNoInteractions(repository, entityManager);
    }

    @Test
    void rejectsDuplicateIdsBeforeWriting() throws Exception {
        var places = json.readTree("""
                [{"id":"a","name":"Gallery","type":"museum","city":"Florence"},
                 {"id":"a","name":"Market","type":"market","city":"Rome"}]
                """);

        assertThatThrownBy(() -> importer.importPlaces(places))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Place 2. Duplicate ID a");
        verifyNoInteractions(repository, entityManager);
    }

    @Test
    void preservesOptionalFieldsAndSourceNotes() throws Exception {
        var places = json.readTree("""
                [{"id":"a","name":"Gallery","type":"museum","city":"Florence",
                  "hours":null,"seasonal_notes":"Hours vary","extra":{"source":"catalog"}}]
                """);
        when(entityManager.find(PlaceImportLock.class, PlaceImportLock.CATALOG_ID, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(new PlaceImportLock());

        assertThat(importer.importPlaces(places)).isEqualTo(1);
        var saved = ArgumentCaptor.forClass(Place.class);
        verify(entityManager).persist(saved.capture());
        assertThat(saved.getValue().data()).isEqualTo(places.get(0));
    }
}
