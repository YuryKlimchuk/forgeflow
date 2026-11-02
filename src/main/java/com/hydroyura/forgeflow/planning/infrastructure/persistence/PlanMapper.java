package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;

public final class PlanMapper {

    public PlanEntity toEntity(Plan plan) {
        PlanEntity entity = new PlanEntity();
        entity.id = plan.getId().value();
        entity.organizationId = plan.getOrganizationId().value();
        entity.name = plan.getName();
        entity.year = plan.getYear();
        entity.month = plan.getMonth();
        return entity;
    }

    public List<PlanItemEntity> toItemEntities(Plan plan) {
        return plan.items().stream()
                .map(item -> {
                    PlanItemEntity entity = new PlanItemEntity();
                    entity.planId = plan.getId().value();
                    entity.assemblyId = item.assemblyId().value();
                    entity.quantity = item.quantity();
                    return entity;
                })
                .toList();
    }

    public Plan toDomain(PlanEntity entity, List<PlanItemEntity> items) {
        Plan plan = Plan.create(
                PlanId.of(entity.id),
                OrganizationId.of(entity.organizationId),
                entity.name,
                entity.year,
                entity.month);
        for (PlanItemEntity item : items) {
            plan.addItem(AssemblyId.of(item.assemblyId), item.quantity);
        }
        return plan;
    }

    public static final PlanMapper INSTANCE = new PlanMapper();

    private PlanMapper() {
    }
}
