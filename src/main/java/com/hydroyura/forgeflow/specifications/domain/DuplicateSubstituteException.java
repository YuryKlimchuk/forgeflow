package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Такой заменитель уже добавлен к строке спецификации.
 */
public class DuplicateSubstituteException extends SpecificationsException {

    public DuplicateSubstituteException(CatalogItemReference substitute) {
        super("Substitute already present: " + substitute.type() + " " + substitute.id());
    }
}
