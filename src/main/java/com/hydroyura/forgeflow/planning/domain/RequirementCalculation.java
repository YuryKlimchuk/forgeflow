package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Сохранённый расчёт потребностей производства: неизменяемый снапшот позиций
 * с моментом выполнения, привязанный к плану и организации.
 */
@Entity
public class RequirementCalculation {

    @Identity
    private final RequirementCalculationId id;

    private final OrganizationId organizationId;
    private final PlanId planId;
    private final Instant executedAt;
    private final List<RequirementItem> items;

    private RequirementCalculation(
            RequirementCalculationId id,
            OrganizationId organizationId,
            PlanId planId,
            Instant executedAt,
            List<RequirementItem> items) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.planId = Objects.requireNonNull(planId, "planId must not be null");
        this.executedAt = Objects.requireNonNull(executedAt, "executedAt must not be null");
        this.items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }

    public static RequirementCalculation create(
            RequirementCalculationId id,
            OrganizationId organizationId,
            PlanId planId,
            Instant executedAt,
            List<RequirementItem> items) {
        return new RequirementCalculation(id, organizationId, planId, executedAt, items);
    }

    public RequirementCalculationId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public PlanId getPlanId() {
        return planId;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public List<RequirementItem> items() {
        return Collections.unmodifiableList(items);
    }
}
