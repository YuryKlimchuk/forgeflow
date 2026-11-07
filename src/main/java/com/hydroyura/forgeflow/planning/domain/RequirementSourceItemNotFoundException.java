package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Элемент источника (головная сборка плана или лист каталога) отсутствует,
 * удалён или принадлежит другой организации.
 */
public class RequirementSourceItemNotFoundException extends PlansException {

    private final CatalogItemReference item;

    public RequirementSourceItemNotFoundException(CatalogItemReference item) {
        super("Requirement source item not found: " + item.type() + " " + item.id());
        this.item = item;
    }

    public CatalogItemReference item() {
        return item;
    }
}
