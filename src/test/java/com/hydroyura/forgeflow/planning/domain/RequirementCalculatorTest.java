package com.hydroyura.forgeflow.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.infrastructure.FakeRequirementSource;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementCalculatorTest {

    private final RequirementCalculator calculator = new RequirementCalculator();
    private final OrganizationId org = OrganizationId.generate();

    private static Plan plan(OrganizationId org, Seed... seeds) {
        Plan plan = Plan.create(PlanId.generate(), org, "План", 2027, 1);
        for (Seed seed : seeds) {
            plan.addItem(seed.assembly(), seed.quantity());
        }
        return plan;
    }

    private record Seed(AssemblyId assembly, int quantity) {
    }

    private static void assertItem(
            List<RequirementItem> items,
            CatalogItemType type,
            UUID id,
            int quantity,
            NeedType needType,
            AssemblyId... sources) {
        assertThat(items).anySatisfy(item -> {
            assertThat(item.type()).isEqualTo(type);
            assertThat(item.id()).isEqualTo(id);
            assertThat(item.quantity()).isEqualTo(quantity);
            assertThat(item.needType()).isEqualTo(needType);
            assertThat(item.unit()).isEqualTo("шт");
            assertThat(item.sources()).containsExactlyInAnyOrder(sources);
        });
    }

    @Test
    void calculatesMultiLevelBomIncludingAssemblies() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        UUID part = UUID.randomUUID();
        UUID buy = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .assembly(b, "СБ-0002", "Узел")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .descriptor(CatalogItemType.BUY, buy, "П-0001", "Подшипник")
                .rows(a,
                        FakeRequirementSource.row(CatalogItemType.PART, part, 3),
                        FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 2))
                .rows(b, FakeRequirementSource.row(CatalogItemType.BUY, buy, 4));

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 2)), source);

        assertThat(result).hasSize(4);
        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 2, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, b.value(), 4, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.PART, part, 6, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.BUY, buy, 16, NeedType.PURCHASE, a);
    }

    @Test
    void includesAssemblyWithEmptySpecification() {
        AssemblyId a = AssemblyId.generate();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос");

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 5)), source);

        assertThat(result).hasSize(1);
        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 5, NeedType.MANUFACTURE, a);
    }

    @Test
    void aggregatesAcrossPlanAssembliesWithMergedSources() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        UUID part = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .assembly(b, "СБ-0002", "Пульт")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 1))
                .rows(b, FakeRequirementSource.row(CatalogItemType.PART, part, 1));

        List<RequirementItem> result = calculator.calculate(
                plan(org, new Seed(a, 2), new Seed(b, 1)), source);

        assertItem(result, CatalogItemType.PART, part, 3, NeedType.MANUFACTURE, a, b);
        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 2, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, b.value(), 1, NeedType.MANUFACTURE, b);
    }

    @Test
    void determinesNeedTypeByCatalogType() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId nested = AssemblyId.generate();
        UUID part = UUID.randomUUID();
        UUID buy = UUID.randomUUID();
        UUID standart = UUID.randomUUID();
        UUID vzk = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .assembly(nested, "СБ-0002", "Узел")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .descriptor(CatalogItemType.BUY, buy, "П-0001", "Подшипник")
                .descriptor(CatalogItemType.STANDART, standart, "С-0001", "Болт")
                .descriptor(CatalogItemType.VZK, vzk, "В-0001", "Группа ВЗК")
                .rows(a,
                        FakeRequirementSource.row(CatalogItemType.PART, part, 1),
                        FakeRequirementSource.row(CatalogItemType.BUY, buy, 1),
                        FakeRequirementSource.row(CatalogItemType.STANDART, standart, 1),
                        FakeRequirementSource.row(CatalogItemType.VZK, vzk, 1),
                        FakeRequirementSource.row(CatalogItemType.ASSEMBLY, nested.value(), 1));

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 1)), source);

        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 1, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, nested.value(), 1, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.PART, part, 1, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.BUY, buy, 1, NeedType.PURCHASE, a);
        assertItem(result, CatalogItemType.STANDART, standart, 1, NeedType.PURCHASE, a);
        assertItem(result, CatalogItemType.VZK, vzk, 1, NeedType.PURCHASE, a);
    }

    @Test
    void emptyPlanReturnsEmptyResult() {
        List<RequirementItem> result = calculator.calculate(
                plan(org), new FakeRequirementSource());

        assertThat(result).isEmpty();
    }

    @Test
    void detectsIndirectCycle() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        AssemblyId c = AssemblyId.generate();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "A")
                .assembly(b, "СБ-0002", "B")
                .assembly(c, "СБ-0003", "C")
                .rows(a, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 1))
                .rows(b, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, c.value(), 1))
                .rows(c, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, a.value(), 1));

        assertThatThrownBy(() -> calculator.calculate(plan(org, new Seed(a, 1)), source))
                .isInstanceOf(RequirementCycleException.class);
    }

    @Test
    void rejectsOverflowOnMultiplication() {
        AssemblyId a = AssemblyId.generate();
        UUID part = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 2));

        assertThatThrownBy(() -> calculator.calculate(
                plan(org, new Seed(a, Integer.MAX_VALUE)), source))
                .isInstanceOf(RequirementQuantityOverflowException.class);
    }

    @Test
    void rejectsOverflowOnAggregation() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        UUID part = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .assembly(b, "СБ-0002", "Пульт")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, Integer.MAX_VALUE))
                .rows(b, FakeRequirementSource.row(CatalogItemType.PART, part, 1));

        assertThatThrownBy(() -> calculator.calculate(
                plan(org, new Seed(a, 1), new Seed(b, 1)), source))
                .isInstanceOf(RequirementQuantityOverflowException.class);
    }

    @Test
    void missingHeadAssemblyThrowsSourceItemNotFound() {
        AssemblyId a = AssemblyId.generate();

        assertThatThrownBy(() -> calculator.calculate(
                plan(org, new Seed(a, 1)), new FakeRequirementSource()))
                .isInstanceOf(RequirementSourceItemNotFoundException.class);
    }

    @Test
    void danglingNestedAssemblyThrowsSpecificationItemNotFound() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .rows(a, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 1));

        assertThatThrownBy(() -> calculator.calculate(plan(org, new Seed(a, 1)), source))
                .isInstanceOfSatisfying(SpecificationItemNotFoundException.class, e -> {
                    assertThat(e.item().type()).isEqualTo(CatalogItemType.ASSEMBLY);
                    assertThat(e.item().id()).isEqualTo(b.value());
                });
    }

    @Test
    void missingLeafThrowsSourceItemNotFound() {
        AssemblyId a = AssemblyId.generate();
        UUID part = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 1));

        assertThatThrownBy(() -> calculator.calculate(plan(org, new Seed(a, 1)), source))
                .isInstanceOfSatisfying(RequirementSourceItemNotFoundException.class, e -> {
                    assertThat(e.item().type()).isEqualTo(CatalogItemType.PART);
                    assertThat(e.item().id()).isEqualTo(part);
                });
    }

    @Test
    void diamondDoesNotTriggerFalseCycle() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        AssemblyId c = AssemblyId.generate();
        AssemblyId d = AssemblyId.generate();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "A")
                .assembly(b, "СБ-0002", "B")
                .assembly(c, "СБ-0003", "C")
                .assembly(d, "СБ-0004", "D")
                .rows(a,
                        FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 1),
                        FakeRequirementSource.row(CatalogItemType.ASSEMBLY, c.value(), 1))
                .rows(b, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, d.value(), 1))
                .rows(c, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, d.value(), 1));

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 1)), source);

        assertItem(result, CatalogItemType.ASSEMBLY, d.value(), 2, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 1, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, b.value(), 1, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, c.value(), 1, NeedType.MANUFACTURE, a);
    }

    @Test
    void acceptsQuantityAtIntMax() {
        AssemblyId a = AssemblyId.generate();
        UUID part = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 1));

        List<RequirementItem> result = calculator.calculate(
                plan(org, new Seed(a, Integer.MAX_VALUE)), source);

        assertItem(result, CatalogItemType.PART, part, Integer.MAX_VALUE, NeedType.MANUFACTURE, a);
    }

    @Test
    void singleLevelPlan() {
        AssemblyId a = AssemblyId.generate();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос");

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 1)), source);

        assertThat(result).hasSize(1);
        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 1, NeedType.MANUFACTURE, a);
    }

    @Test
    void sumExactlyAtIntMaxIsAccepted() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        UUID part = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .assembly(b, "СБ-0002", "Пульт")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 1))
                .rows(b, FakeRequirementSource.row(CatalogItemType.PART, part, Integer.MAX_VALUE - 1));

        List<RequirementItem> result = calculator.calculate(
                plan(org, new Seed(a, 1), new Seed(b, 1)), source);

        assertItem(result, CatalogItemType.PART, part, Integer.MAX_VALUE, NeedType.MANUFACTURE, a, b);
    }

    @Test
    void deepChainMultipliesAcrossLevels() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        AssemblyId c = AssemblyId.generate();
        AssemblyId d = AssemblyId.generate();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "A")
                .assembly(b, "СБ-0002", "B")
                .assembly(c, "СБ-0003", "C")
                .assembly(d, "СБ-0004", "D")
                .rows(a, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 3))
                .rows(b, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, c.value(), 4))
                .rows(c, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, d.value(), 5));

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 2)), source);

        assertItem(result, CatalogItemType.ASSEMBLY, a.value(), 2, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, b.value(), 6, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, c.value(), 24, NeedType.MANUFACTURE, a);
        assertItem(result, CatalogItemType.ASSEMBLY, d.value(), 120, NeedType.MANUFACTURE, a);
    }

    @Test
    void sortsByNumberThenTypeThenId() {
        AssemblyId a = AssemblyId.generate();
        UUID part1 = UUID.randomUUID();
        UUID part2 = UUID.randomUUID();
        UUID buy = UUID.randomUUID();

        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0000", "Корень")
                .descriptor(CatalogItemType.PART, part1, "Д-0001", "Деталь 1")
                .descriptor(CatalogItemType.PART, part2, "Д-0001", "Деталь 2")
                .descriptor(CatalogItemType.BUY, buy, "Д-0001", "Покупная")
                .rows(a,
                        FakeRequirementSource.row(CatalogItemType.BUY, buy, 1),
                        FakeRequirementSource.row(CatalogItemType.PART, part2, 1),
                        FakeRequirementSource.row(CatalogItemType.PART, part1, 1));

        List<RequirementItem> result = calculator.calculate(plan(org, new Seed(a, 1)), source);

        assertThat(result).extracting(RequirementItem::number)
                .containsExactly("Д-0001", "Д-0001", "Д-0001", "СБ-0000");
        assertThat(result.get(0).type()).isEqualTo(CatalogItemType.PART);
        assertThat(result.get(0).id()).isEqualTo(smallerId(part1, part2));
        assertThat(result.get(1).type()).isEqualTo(CatalogItemType.PART);
        assertThat(result.get(1).id()).isEqualTo(largerId(part1, part2));
        assertThat(result.get(2).type()).isEqualTo(CatalogItemType.BUY);
        assertThat(result.get(3).type()).isEqualTo(CatalogItemType.ASSEMBLY);
    }

    private static UUID smallerId(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a : b;
    }

    private static UUID largerId(UUID a, UUID b) {
        return a.compareTo(b) > 0 ? a : b;
    }
}
