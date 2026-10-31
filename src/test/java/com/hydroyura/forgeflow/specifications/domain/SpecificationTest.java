package com.hydroyura.forgeflow.specifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpecificationTest {

    private final OrganizationId orgId = OrganizationId.generate();
    private final AssemblyId assemblyId = AssemblyId.generate();

    private static CatalogItemReference part() {
        return new CatalogItemReference(CatalogItemType.PART, UUID.randomUUID());
    }

    @Test
    void addRowStoresQuantity() {
        Specification spec = Specification.create(orgId, assemblyId);
        CatalogItemReference item = part();

        spec.addRow(item, new Quantity(3));

        assertThat(spec.rows()).hasSize(1);
        assertThat(spec.rows().get(0).item()).isEqualTo(item);
        assertThat(spec.rows().get(0).quantity().value()).isEqualTo(3);
    }

    @Test
    void addRowRejectsDuplicateItem() {
        Specification spec = Specification.create(orgId, assemblyId);
        CatalogItemReference item = part();
        spec.addRow(item, new Quantity(1));

        assertThatThrownBy(() -> spec.addRow(item, new Quantity(5)))
                .isInstanceOf(DuplicateItemException.class);
        assertThat(spec.rows()).hasSize(1);
        assertThat(spec.rows().get(0).quantity().value()).isEqualTo(1);
    }

    @Test
    void addRowRejectsSelfReference() {
        Specification spec = Specification.create(orgId, assemblyId);
        CatalogItemReference self = new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyId.value());

        assertThatThrownBy(() -> spec.addRow(self, new Quantity(1)))
                .isInstanceOf(SelfReferenceException.class);
        assertThat(spec.rows()).isEmpty();
    }

    @Test
    void changeQuantityUpdatesRow() {
        Specification spec = Specification.create(orgId, assemblyId);
        CatalogItemReference item = part();
        spec.addRow(item, new Quantity(1));

        spec.changeQuantity(item, new Quantity(9));

        assertThat(spec.rows().get(0).quantity().value()).isEqualTo(9);
    }

    @Test
    void changeQuantityRejectsMissingRow() {
        Specification spec = Specification.create(orgId, assemblyId);

        assertThatThrownBy(() -> spec.changeQuantity(part(), new Quantity(1)))
                .isInstanceOf(RowNotFoundException.class);
    }

    @Test
    void removeRowRemoves() {
        Specification spec = Specification.create(orgId, assemblyId);
        CatalogItemReference item = part();
        spec.addRow(item, new Quantity(1));

        spec.removeRow(item);

        assertThat(spec.rows()).isEmpty();
    }

    @Test
    void removeRowRejectsMissingRow() {
        Specification spec = Specification.create(orgId, assemblyId);

        assertThatThrownBy(() -> spec.removeRow(part()))
                .isInstanceOf(RowNotFoundException.class);
    }

    @Test
    void indirectCycleIsNotChecked() {
        Specification spec = Specification.create(orgId, assemblyId);
        CatalogItemReference otherAssembly = new CatalogItemReference(CatalogItemType.ASSEMBLY, UUID.randomUUID());

        spec.addRow(otherAssembly, new Quantity(1));

        assertThat(spec.rows()).hasSize(1);
    }
}
