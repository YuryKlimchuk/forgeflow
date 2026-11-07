package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementCalculationMapperTest {

    private static final Instant EXECUTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private final RequirementCalculationMapper mapper = RequirementCalculationMapper.INSTANCE;

    private static RequirementItem item(
            CatalogItemType type, UUID id, String number, String name, int quantity,
            NeedType needType, AssemblyId... sources) {
        return new RequirementItem(type, id, number, name, quantity, needType, "шт", List.of(sources));
    }

    @Test
    void roundTripPreservesAllFields() {
        AssemblyId source = AssemblyId.generate();
        RequirementCalculationId id = RequirementCalculationId.generate();
        OrganizationId org = OrganizationId.generate();
        PlanId planId = PlanId.generate();
        UUID partId = UUID.randomUUID();

        RequirementCalculation calculation = RequirementCalculation.create(
                id, org, planId, EXECUTED_AT,
                List.of(item(CatalogItemType.PART, partId, "Д-0001", "Втулка", 3, NeedType.MANUFACTURE, source)));

        RequirementCalculationEntity entity = mapper.toEntity(calculation);
        RequirementCalculationItemEntity itemEntity = mapper.toItemEntity(entity.id, calculation.items().get(0));
        RequirementCalculationItemSourceEntity sourceEntity = mapper.toSourceEntity(itemEntity.id, source);

        RequirementCalculation restored = mapper.toDomain(entity, List.of(itemEntity), List.of(sourceEntity));

        assertThat(restored.getId()).isEqualTo(id);
        assertThat(restored.getOrganizationId()).isEqualTo(org);
        assertThat(restored.getPlanId()).isEqualTo(planId);
        assertThat(restored.getExecutedAt()).isEqualTo(EXECUTED_AT);
        assertThat(restored.items()).hasSize(1);
        assertThat(restored.items().get(0).type()).isEqualTo(CatalogItemType.PART);
        assertThat(restored.items().get(0).id()).isEqualTo(partId);
        assertThat(restored.items().get(0).number()).isEqualTo("Д-0001");
        assertThat(restored.items().get(0).name()).isEqualTo("Втулка");
        assertThat(restored.items().get(0).quantity()).isEqualTo(3);
        assertThat(restored.items().get(0).needType()).isEqualTo(NeedType.MANUFACTURE);
        assertThat(restored.items().get(0).unit()).isEqualTo("шт");
        assertThat(restored.items().get(0).sources()).containsExactly(source);
    }

    @Test
    void restoresDeterministicItemOrderAndSortsSources() {
        AssemblyId s1 = AssemblyId.generate();
        AssemblyId s2 = AssemblyId.generate();
        UUID partA = UUID.randomUUID();
        UUID buyB = UUID.randomUUID();

        RequirementItem assembly = item(CatalogItemType.ASSEMBLY, UUID.randomUUID(), "СБ-0001", "Сборка", 1, NeedType.MANUFACTURE, s1);
        RequirementItem buy = item(CatalogItemType.BUY, buyB, "Д-0001", "Покупная", 1, NeedType.PURCHASE, s1);
        RequirementItem part = item(CatalogItemType.PART, partA, "Д-0001", "Деталь", 1, NeedType.MANUFACTURE, s2, s1);

        RequirementCalculation calculation = RequirementCalculation.create(
                RequirementCalculationId.generate(), OrganizationId.generate(), PlanId.generate(),
                EXECUTED_AT, List.of(assembly, buy, part));

        RequirementCalculationEntity entity = mapper.toEntity(calculation);
        List<RequirementCalculationItemEntity> itemEntities = new ArrayList<>();
        List<RequirementCalculationItemSourceEntity> sourceEntities = new ArrayList<>();
        for (RequirementItem item : calculation.items()) {
            RequirementCalculationItemEntity itemEntity = mapper.toItemEntity(entity.id, item);
            itemEntities.add(itemEntity);
            for (AssemblyId source : item.sources()) {
                sourceEntities.add(mapper.toSourceEntity(itemEntity.id, source));
            }
        }

        RequirementCalculation restored = mapper.toDomain(entity, itemEntities, sourceEntities);

        assertThat(restored.items()).extracting(RequirementItem::number)
                .containsExactly("Д-0001", "Д-0001", "СБ-0001");
        assertThat(restored.items().get(0).type()).isEqualTo(CatalogItemType.PART);
        assertThat(restored.items().get(1).type()).isEqualTo(CatalogItemType.BUY);
        assertThat(restored.items().get(2).type()).isEqualTo(CatalogItemType.ASSEMBLY);
        List<AssemblyId> expectedSources = java.util.stream.Stream.of(s1, s2)
                .sorted(java.util.Comparator.comparing(AssemblyId::value))
                .toList();
        assertThat(restored.items().get(0).sources()).containsExactlyElementsOf(expectedSources);
    }

    @Test
    void roundTripAllowsEmptyItems() {
        RequirementCalculation calculation = RequirementCalculation.create(
                RequirementCalculationId.generate(), OrganizationId.generate(), PlanId.generate(),
                EXECUTED_AT, List.of());

        RequirementCalculationEntity entity = mapper.toEntity(calculation);

        RequirementCalculation restored = mapper.toDomain(entity, List.of(), List.of());

        assertThat(restored.items()).isEmpty();
    }
}
