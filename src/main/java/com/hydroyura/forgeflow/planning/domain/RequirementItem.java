package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 * Позиция расчёта потребностей: агрегированный по паре «тип + id» элемент
 * каталога со снапшотом номера/наименования, количеством, видом потребности,
 * единицей и набором сборок плана-источников.
 */
public record RequirementItem(
        CatalogItemType type,
        UUID id,
        String number,
        String name,
        int quantity,
        NeedType needType,
        String unit,
        List<AssemblyId> sources) {

    public RequirementItem {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
        if (StringUtils.isBlank(number)) {
            throw new IllegalArgumentException("number must not be blank");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Objects.requireNonNull(needType, "needType must not be null");
        if (StringUtils.isBlank(unit)) {
            throw new IllegalArgumentException("unit must not be blank");
        }
        Objects.requireNonNull(sources, "sources must not be null");
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("sources must not be empty");
        }
        sources = List.copyOf(sources);
        if (new HashSet<>(sources).size() != sources.size()) {
            throw new IllegalArgumentException("sources must not contain duplicates");
        }
    }
}
