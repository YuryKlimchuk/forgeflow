package com.hydroyura.forgeflow.specifications.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Дискриминированная ссылка на элемент каталога: тип и идентификатор.
 */
public record CatalogItemReference(CatalogItemType type, UUID id) {

    public CatalogItemReference {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }
}
