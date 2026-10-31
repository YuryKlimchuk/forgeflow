package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.PartId;
import org.junit.jupiter.api.Test;

class CatalogExceptionTest {

    @Test
    void hierarchy_isRootedAtCatalogException() {
        assertThat(CatalogException.class.isAssignableFrom(PartNotFoundException.class)).isTrue();
        assertThat(CatalogException.class.isAssignableFrom(DuplicatePartNumberException.class)).isTrue();
        assertThat(RuntimeException.class.isAssignableFrom(CatalogException.class)).isTrue();
    }

    @Test
    void partNotFound_carriesId() {
        PartId id = PartId.generate();
        PartNotFoundException ex = new PartNotFoundException(id);
        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    void duplicateNumber_carriesNumber() {
        DuplicatePartNumberException ex = new DuplicatePartNumberException("P-1");
        assertThat(ex.getMessage()).contains("P-1");
    }
}
