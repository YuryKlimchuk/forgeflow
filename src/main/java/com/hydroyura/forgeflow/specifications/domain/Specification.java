package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Спецификация сборочной единицы: прямые строки состава.
 *
 * <p>Агрегат адресуется парой организация + сборочная единица и материализуется
 * логически: пустая спецификация — это набор из нуля строк.</p>
 */
@Entity
public class Specification {

    private final OrganizationId organizationId;

    @Identity
    private final AssemblyId assemblyId;

    private final List<SpecificationRow> rows = new ArrayList<>();

    private Specification(OrganizationId organizationId, AssemblyId assemblyId) {
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.assemblyId = Objects.requireNonNull(assemblyId, "assemblyId must not be null");
    }

    public static Specification create(OrganizationId organizationId, AssemblyId assemblyId) {
        return new Specification(organizationId, assemblyId);
    }

    public void addRow(CatalogItemReference item, Quantity quantity) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        if (item.type() == CatalogItemType.ASSEMBLY && item.id().equals(assemblyId.value())) {
            throw new SelfReferenceException(assemblyId);
        }
        if (rows.stream().anyMatch(row -> row.item().equals(item))) {
            throw new DuplicateItemException(item);
        }
        rows.add(new SpecificationRow(item, quantity));
    }

    public void removeRow(CatalogItemReference item) {
        Objects.requireNonNull(item, "item must not be null");
        SpecificationRow row = findRow(item)
                .orElseThrow(() -> new RowNotFoundException(item));
        rows.remove(row);
    }

    public void changeQuantity(CatalogItemReference item, Quantity quantity) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        SpecificationRow row = findRow(item)
                .orElseThrow(() -> new RowNotFoundException(item));
        rows.set(rows.indexOf(row), new SpecificationRow(item, quantity));
    }

    public List<SpecificationRow> rows() {
        return Collections.unmodifiableList(rows);
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public AssemblyId assemblyId() {
        return assemblyId;
    }

    private Optional<SpecificationRow> findRow(CatalogItemReference item) {
        return rows.stream()
                .filter(row -> row.item().equals(item))
                .findFirst();
    }
}
