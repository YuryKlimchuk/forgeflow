package com.hydroyura.forgeflow.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementItemTest {

    private static final UUID ITEM_ID = UUID.randomUUID();

    private RequirementItem item() {
        return new RequirementItem(
                CatalogItemType.PART, ITEM_ID, "Д-0001", "Втулка", 3,
                NeedType.MANUFACTURE, "шт", List.of(AssemblyId.generate()));
    }

    @Test
    void holdsAllFields() {
        AssemblyId source = AssemblyId.generate();
        RequirementItem item = new RequirementItem(
                CatalogItemType.ASSEMBLY, ITEM_ID, "СБ-0001", "Насос", 5,
                NeedType.MANUFACTURE, "шт", List.of(source));

        assertThat(item.type()).isEqualTo(CatalogItemType.ASSEMBLY);
        assertThat(item.id()).isEqualTo(ITEM_ID);
        assertThat(item.number()).isEqualTo("СБ-0001");
        assertThat(item.name()).isEqualTo("Насос");
        assertThat(item.quantity()).isEqualTo(5);
        assertThat(item.needType()).isEqualTo(NeedType.MANUFACTURE);
        assertThat(item.unit()).isEqualTo("шт");
        assertThat(item.sources()).containsExactly(source);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new RequirementItem(
                CatalogItemType.PART, ITEM_ID, "Д-0001", "Втулка", 0,
                NeedType.MANUFACTURE, "шт", List.of(AssemblyId.generate())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankNumberNameOrUnit() {
        AssemblyId source = AssemblyId.generate();
        assertThatThrownBy(() -> new RequirementItem(
                CatalogItemType.PART, ITEM_ID, " ", "Втулка", 1, NeedType.MANUFACTURE, "шт", List.of(source)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequirementItem(
                CatalogItemType.PART, ITEM_ID, "Д-0001", " ", 1, NeedType.MANUFACTURE, "шт", List.of(source)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequirementItem(
                CatalogItemType.PART, ITEM_ID, "Д-0001", "Втулка", 1, NeedType.MANUFACTURE, " ", List.of(source)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyOrDuplicateSources() {
        assertThatThrownBy(() -> new RequirementItem(
                CatalogItemType.PART, ITEM_ID, "Д-0001", "Втулка", 1,
                NeedType.MANUFACTURE, "шт", List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        AssemblyId source = AssemblyId.generate();
        assertThatThrownBy(() -> new RequirementItem(
                CatalogItemType.PART, ITEM_ID, "Д-0001", "Втулка", 1,
                NeedType.MANUFACTURE, "шт", List.of(source, source)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sourcesAreImmutable() {
        RequirementItem item = item();

        assertThatThrownBy(() -> item.sources().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
