package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Производственный план на календарный месяц: имя, период (год + месяц) и
 * состав сборочных единиц с количеством.
 */
@Entity
public class Plan {

    @Identity
    private final PlanId id;

    private final OrganizationId organizationId;
    private final String name;
    private final int year;
    private final int month;

    private final List<PlanItem> items = new ArrayList<>();

    private Plan(PlanId id, OrganizationId organizationId, String name, int year, int month) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.name = requireNotBlank(name);
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        this.year = year;
        this.month = month;
    }

    public static Plan create(PlanId id, OrganizationId organizationId, String name, int year, int month) {
        return new Plan(id, organizationId, name, year, month);
    }

    public void addItem(AssemblyId assemblyId, int quantity) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (items.stream().anyMatch(item -> item.assemblyId().equals(assemblyId))) {
            throw new DuplicatePlanItemException(assemblyId);
        }
        items.add(new PlanItem(assemblyId, quantity));
    }

    public void changeQuantity(AssemblyId assemblyId, int quantity) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        PlanItem item = findItem(assemblyId)
                .orElseThrow(() -> new PlanItemNotFoundException(assemblyId));
        items.set(items.indexOf(item), new PlanItem(assemblyId, quantity));
    }

    public void removeItem(AssemblyId assemblyId) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        PlanItem item = findItem(assemblyId)
                .orElseThrow(() -> new PlanItemNotFoundException(assemblyId));
        items.remove(item);
    }

    public List<PlanItem> items() {
        return Collections.unmodifiableList(items);
    }

    public PlanId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    private Optional<PlanItem> findItem(AssemblyId assemblyId) {
        return items.stream()
                .filter(item -> item.assemblyId().equals(assemblyId))
                .findFirst();
    }

    private static String requireNotBlank(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return value;
    }
}
