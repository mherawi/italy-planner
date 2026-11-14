package com.italyplanner;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "place_import_lock")
public class PlaceImportLock {
    static final int CATALOG_ID = 1;

    @Id
    private Integer id;

    protected PlaceImportLock() {
    }
}
