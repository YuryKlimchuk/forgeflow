package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Доменный сервис рекурсивного разузлования спецификаций плана.
 *
 * <p>Чистая функция без Spring/JPA/внешних сервисов: данные приходят через
 * {@link RequirementSource}. Обход учитывает только основные строки
 * спецификаций (заменители не видны порту), обнаруживает циклы и переполнение
 * количества, агрегирует одинаковые элементы по паре «тип + id» и сортирует
 * результат детерминированно.</p>
 */
public class RequirementCalculator {

    private static final String UNIT = "шт";

    private static final Comparator<RequirementItem> ITEM_ORDER =
            Comparator.comparing(RequirementItem::number)
                    .thenComparing(RequirementItem::type)
                    .thenComparing(RequirementItem::id);

    public List<RequirementItem> calculate(Plan plan, RequirementSource source) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(source, "source must not be null");

        Map<CatalogItemReference, Accumulator> accumulators = new LinkedHashMap<>();
        for (PlanItem planItem : plan.items()) {
            traverse(planItem.assemblyId(), planItem.quantity(), planItem.assemblyId(), true,
                    new LinkedHashSet<>(), source, accumulators);
        }

        return accumulators.values().stream()
                .map(this::toItem)
                .sorted(ITEM_ORDER)
                .toList();
    }

    private void traverse(
            AssemblyId assemblyId,
            int multiplier,
            AssemblyId rootAssemblyId,
            boolean head,
            Set<AssemblyId> path,
            RequirementSource source,
            Map<CatalogItemReference, Accumulator> accumulators) {
        if (!path.add(assemblyId)) {
            throw new RequirementCycleException(assemblyId);
        }
        try {
            CatalogItemReference assemblyRef = new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyId.value());
            RequirementSource.ItemDescriptor descriptor = source.descriptor(assemblyRef)
                    .orElseThrow(() -> head
                            ? new RequirementSourceItemNotFoundException(assemblyRef)
                            : new SpecificationItemNotFoundException(assemblyRef));

            emit(assemblyRef, descriptor, multiplier, NeedType.MANUFACTURE, rootAssemblyId, accumulators);

            for (RequirementSource.SourceRow row : source.rows(assemblyId)) {
                int rowQuantity = multiplyExact(multiplier, row.quantity());
                if (row.item().type() == CatalogItemType.ASSEMBLY) {
                    traverse(AssemblyId.of(row.item().id()), rowQuantity, rootAssemblyId, false,
                            path, source, accumulators);
                } else {
                    RequirementSource.ItemDescriptor itemDescriptor = source.descriptor(row.item())
                            .orElseThrow(() -> new RequirementSourceItemNotFoundException(row.item()));
                    emit(row.item(), itemDescriptor, rowQuantity, needType(row.item().type()),
                            rootAssemblyId, accumulators);
                }
            }
        } finally {
            path.remove(assemblyId);
        }
    }

    private void emit(
            CatalogItemReference reference,
            RequirementSource.ItemDescriptor descriptor,
            int quantity,
            NeedType needType,
            AssemblyId rootAssemblyId,
            Map<CatalogItemReference, Accumulator> accumulators) {
        Accumulator accumulator = accumulators.get(reference);
        if (accumulator == null) {
            accumulator = new Accumulator(
                    reference.type(), reference.id(), descriptor.number(), descriptor.name(), needType);
            accumulators.put(reference, accumulator);
        }
        accumulator.add(quantity, rootAssemblyId);
    }

    private RequirementItem toItem(Accumulator accumulator) {
        return new RequirementItem(
                accumulator.type,
                accumulator.id,
                accumulator.number,
                accumulator.name,
                accumulator.quantity,
                accumulator.needType,
                UNIT,
                List.copyOf(accumulator.sources));
    }

    private static NeedType needType(CatalogItemType type) {
        return switch (type) {
            case ASSEMBLY, PART -> NeedType.MANUFACTURE;
            case BUY, STANDART, VZK -> NeedType.PURCHASE;
        };
    }

    private static int multiplyExact(int a, int b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            throw new RequirementQuantityOverflowException("quantity overflow when multiplying " + a + " by " + b);
        }
    }

    private static final class Accumulator {

        private final CatalogItemType type;
        private final java.util.UUID id;
        private final String number;
        private final String name;
        private final NeedType needType;
        private int quantity;
        private final Set<AssemblyId> sources = new LinkedHashSet<>();

        private Accumulator(
                CatalogItemType type,
                java.util.UUID id,
                String number,
                String name,
                NeedType needType) {
            this.type = type;
            this.id = id;
            this.number = number;
            this.name = name;
            this.needType = needType;
        }

        private void add(int value, AssemblyId source) {
            try {
                this.quantity = Math.addExact(this.quantity, value);
            } catch (ArithmeticException e) {
                throw new RequirementQuantityOverflowException("quantity overflow when summing " + this.quantity + " and " + value);
            }
            this.sources.add(source);
        }
    }
}
