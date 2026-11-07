package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Инфраструктурный маппер entity {@literal <->} domain. При реконструкции
 * агрегата восстанавливает детерминированный порядок позиций (number → type →
 * id), тот же, что использует {@link com.hydroyura.forgeflow.planning.domain.RequirementCalculator}.
 */
public final class RequirementCalculationMapper {

    private static final Comparator<RequirementItem> ITEM_ORDER =
            Comparator.comparing(RequirementItem::number)
                    .thenComparing(RequirementItem::type)
                    .thenComparing(RequirementItem::id);

    public RequirementCalculationEntity toEntity(RequirementCalculation calculation) {
        RequirementCalculationEntity entity = new RequirementCalculationEntity();
        entity.id = calculation.getId().value();
        entity.organizationId = calculation.getOrganizationId().value();
        entity.planId = calculation.getPlanId().value();
        entity.executedAt = calculation.getExecutedAt();
        return entity;
    }

    public RequirementCalculationItemEntity toItemEntity(UUID calculationId, RequirementItem item) {
        RequirementCalculationItemEntity entity = new RequirementCalculationItemEntity();
        entity.id = UUID.randomUUID();
        entity.calculationId = calculationId;
        entity.itemType = item.type().name();
        entity.itemId = item.id();
        entity.number = item.number();
        entity.name = item.name();
        entity.quantity = item.quantity();
        entity.needType = item.needType().name();
        entity.unit = item.unit();
        return entity;
    }

    public RequirementCalculationItemSourceEntity toSourceEntity(UUID calculationItemId, AssemblyId source) {
        RequirementCalculationItemSourceEntity entity = new RequirementCalculationItemSourceEntity();
        entity.calculationItemId = calculationItemId;
        entity.sourceAssemblyId = source.value();
        return entity;
    }

    public RequirementCalculation toDomain(
            RequirementCalculationEntity entity,
            List<RequirementCalculationItemEntity> itemEntities,
            List<RequirementCalculationItemSourceEntity> sourceEntities) {
        Map<UUID, List<AssemblyId>> sourcesByItem = sourceEntities.stream()
                .collect(Collectors.groupingBy(
                        source -> source.calculationItemId,
                        Collectors.mapping(
                                source -> AssemblyId.of(source.sourceAssemblyId),
                                Collectors.toList())));

        List<RequirementItem> items = itemEntities.stream()
                .map(item -> toItem(item, sourcesByItem.getOrDefault(item.id, List.of())))
                .sorted(ITEM_ORDER)
                .toList();

        return RequirementCalculation.create(
                RequirementCalculationId.of(entity.id),
                OrganizationId.of(entity.organizationId),
                PlanId.of(entity.planId),
                entity.executedAt,
                items);
    }

    private RequirementItem toItem(RequirementCalculationItemEntity entity, List<AssemblyId> sources) {
        List<AssemblyId> orderedSources = sources.stream()
                .sorted(Comparator.comparing(AssemblyId::value))
                .toList();
        return new RequirementItem(
                CatalogItemType.valueOf(entity.itemType),
                entity.itemId,
                entity.number,
                entity.name,
                entity.quantity,
                NeedType.valueOf(entity.needType),
                entity.unit,
                orderedSources);
    }

    public static final RequirementCalculationMapper INSTANCE = new RequirementCalculationMapper();

    private RequirementCalculationMapper() {
    }
}
