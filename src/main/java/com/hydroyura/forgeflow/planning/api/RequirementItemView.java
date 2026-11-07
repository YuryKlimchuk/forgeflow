package com.hydroyura.forgeflow.planning.api;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.List;
import java.util.UUID;

/**
 * Представление позиции расчёта потребностей.
 */
public record RequirementItemView(
        CatalogItemType type,
        UUID id,
        String number,
        String name,
        int quantity,
        NeedType needType,
        String unit,
        List<AssemblyId> sources) {
}
