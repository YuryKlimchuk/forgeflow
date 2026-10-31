package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Сборочная единица отсутствует, удалена или принадлежит другой организации.
 */
public class AssemblyNotFoundException extends CatalogException {

    public AssemblyNotFoundException(AssemblyId id) {
        super("Assembly not found: " + id);
    }
}
