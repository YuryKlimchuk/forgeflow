package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Заменитель отсутствует в списке замен строки.
 */
public class SubstituteNotFoundException extends SpecificationsException {

    public SubstituteNotFoundException(CatalogItemReference substitute) {
        super("Substitute not found: " + substitute.type() + " " + substitute.id());
    }
}
