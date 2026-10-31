package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Заменитель недопустим: основной элемент не является деталью/сборкой, тип
 * заменителя отличается от типа основной позиции либо заменитель совпадает с
 * основной позицией (самозамена).
 */
public class InvalidSubstituteException extends SpecificationsException {

    public InvalidSubstituteException(CatalogItemReference item, CatalogItemReference substitute) {
        super("Invalid substitute for " + item.type() + " " + item.id()
                + ": " + substitute.type() + " " + substitute.id());
    }
}
