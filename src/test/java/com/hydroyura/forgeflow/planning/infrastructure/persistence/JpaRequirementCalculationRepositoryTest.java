package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.AbstractPostgresIntegrationTest;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationRepository;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class JpaRequirementCalculationRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired
    RequirementCalculationRepository repository;

    @Autowired
    PlanRepository planRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Plan savedPlan(OrganizationId org) {
        Plan plan = Plan.create(PlanId.generate(), org, "План " + UUID.randomUUID(), 2027, 1);
        planRepository.save(plan);
        return plan;
    }

    private static RequirementItem item(
            CatalogItemType type, UUID id, String number, String name, int quantity,
            NeedType needType, AssemblyId... sources) {
        return new RequirementItem(type, id, number, name, quantity, needType, "шт", List.of(sources));
    }

    private static RequirementCalculation calculation(
            OrganizationId org, PlanId planId, Instant executedAt, List<RequirementItem> items) {
        return RequirementCalculation.create(
                RequirementCalculationId.generate(), org, planId, executedAt, items);
    }

    @Test
    void roundTripPreservesAllFields() {
        OrganizationId org = OrganizationId.generate();
        Plan plan = savedPlan(org);
        AssemblyId source = AssemblyId.generate();
        UUID partId = UUID.randomUUID();

        RequirementCalculation saved = calculation(org, plan.getId(), T1,
                List.of(item(CatalogItemType.PART, partId, "Д-0001", "Втулка", 3, NeedType.MANUFACTURE, source)));
        repository.save(saved);

        RequirementCalculation loaded = repository.findById(org, plan.getId(), saved.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getOrganizationId()).isEqualTo(org);
        assertThat(loaded.getPlanId()).isEqualTo(plan.getId());
        assertThat(loaded.getExecutedAt()).isEqualTo(T1);
        assertThat(loaded.items()).hasSize(1);
        assertThat(loaded.items().get(0).type()).isEqualTo(CatalogItemType.PART);
        assertThat(loaded.items().get(0).id()).isEqualTo(partId);
        assertThat(loaded.items().get(0).number()).isEqualTo("Д-0001");
        assertThat(loaded.items().get(0).name()).isEqualTo("Втулка");
        assertThat(loaded.items().get(0).quantity()).isEqualTo(3);
        assertThat(loaded.items().get(0).needType()).isEqualTo(NeedType.MANUFACTURE);
        assertThat(loaded.items().get(0).unit()).isEqualTo("шт");
        assertThat(loaded.items().get(0).sources()).containsExactly(source);
    }

    @Test
    void readsAreTenantScoped() {
        OrganizationId orgA = OrganizationId.generate();
        OrganizationId orgB = OrganizationId.generate();
        Plan plan = savedPlan(orgA);
        AssemblyId source = AssemblyId.generate();

        RequirementCalculation saved = calculation(orgA, plan.getId(), T1,
                List.of(item(CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Втулка", 1, NeedType.MANUFACTURE, source)));
        repository.save(saved);

        assertThat(repository.findByPlanId(orgB, plan.getId())).isEmpty();
        assertThat(repository.findById(orgB, plan.getId(), saved.getId())).isEmpty();
    }

    @Test
    void findByPlanIdReturnsHistoryNewestFirst() {
        OrganizationId org = OrganizationId.generate();
        Plan plan = savedPlan(org);
        AssemblyId source = AssemblyId.generate();

        RequirementCalculation first = calculation(org, plan.getId(), T1,
                List.of(item(CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Втулка", 1, NeedType.MANUFACTURE, source)));
        RequirementCalculation second = calculation(org, plan.getId(), T2,
                List.of(item(CatalogItemType.PART, UUID.randomUUID(), "Д-0002", "Корпус", 2, NeedType.MANUFACTURE, source)));
        repository.save(first);
        repository.save(second);

        List<RequirementCalculation> result = repository.findByPlanId(org, plan.getId());

        assertThat(result).extracting(RequirementCalculation::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void restoresDeterministicItemOrderOnRead() {
        OrganizationId org = OrganizationId.generate();
        Plan plan = savedPlan(org);
        AssemblyId source = AssemblyId.generate();

        RequirementItem assembly = item(CatalogItemType.ASSEMBLY, UUID.randomUUID(), "СБ-0001", "Сборка", 1, NeedType.MANUFACTURE, source);
        RequirementItem buy = item(CatalogItemType.BUY, UUID.randomUUID(), "Д-0001", "Покупная", 1, NeedType.PURCHASE, source);
        RequirementItem part = item(CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Деталь", 1, NeedType.MANUFACTURE, source);

        RequirementCalculation saved = calculation(org, plan.getId(), T1, List.of(assembly, buy, part));
        repository.save(saved);

        RequirementCalculation loaded = repository.findById(org, plan.getId(), saved.getId()).orElseThrow();

        assertThat(loaded.items()).extracting(RequirementItem::number)
                .containsExactly("Д-0001", "Д-0001", "СБ-0001");
        assertThat(loaded.items().get(0).type()).isEqualTo(CatalogItemType.PART);
        assertThat(loaded.items().get(1).type()).isEqualTo(CatalogItemType.BUY);
        assertThat(loaded.items().get(2).type()).isEqualTo(CatalogItemType.ASSEMBLY);
    }

    @Test
    void duplicateItemKeyRejected() {
        OrganizationId org = OrganizationId.generate();
        Plan plan = savedPlan(org);
        AssemblyId source = AssemblyId.generate();
        UUID partId = UUID.randomUUID();

        RequirementCalculation saved = calculation(org, plan.getId(), T1,
                List.of(item(CatalogItemType.PART, partId, "Д-0001", "Втулка", 1, NeedType.MANUFACTURE, source)));
        repository.save(saved);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO requirement_calculation_item
                    (id, calculation_id, item_type, item_id, number, name, quantity, need_type, unit)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), saved.getId().value(), "PART", partId,
                "Д-0001", "Втулка", 1, "MANUFACTURE", "шт"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cascadeRemovesItemsAndSources() {
        OrganizationId org = OrganizationId.generate();
        Plan plan = savedPlan(org);
        AssemblyId source = AssemblyId.generate();

        RequirementCalculation saved = calculation(org, plan.getId(), T1,
                List.of(item(CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Втулка", 1, NeedType.MANUFACTURE, source)));
        repository.save(saved);

        jdbcTemplate.update("DELETE FROM requirement_calculation WHERE id = ?", saved.getId().value());

        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM requirement_calculation_item WHERE calculation_id = ?",
                Integer.class, saved.getId().value());
        Integer sourceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM requirement_calculation_item_source s "
                        + "JOIN requirement_calculation_item i ON i.id = s.calculation_item_id "
                        + "WHERE i.calculation_id = ?",
                Integer.class, saved.getId().value());

        assertThat(itemCount).isZero();
        assertThat(sourceCount).isZero();
    }

    @Test
    void findByIdRequiresMatchingPlan() {
        OrganizationId org = OrganizationId.generate();
        Plan plan = savedPlan(org);
        Plan otherPlan = savedPlan(org);
        AssemblyId source = AssemblyId.generate();

        RequirementCalculation saved = calculation(org, plan.getId(), T1,
                List.of(item(CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Втулка", 1, NeedType.MANUFACTURE, source)));
        repository.save(saved);

        assertThat(repository.findById(org, otherPlan.getId(), saved.getId())).isEmpty();
    }
}
