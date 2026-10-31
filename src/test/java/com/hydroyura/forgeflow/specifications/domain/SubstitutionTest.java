package com.hydroyura.forgeflow.specifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubstitutionTest {

    private static CatalogItemReference part(String id) {
        return new CatalogItemReference(CatalogItemType.PART, UUID.fromString(id));
    }

    private static CatalogItemReference assembly(String id) {
        return new CatalogItemReference(CatalogItemType.ASSEMBLY, UUID.fromString(id));
    }

    @Test
    void addSameTypeSubstituteSucceeds() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000001");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);
        CatalogItemReference substitute = part("00000000-0000-0000-0000-000000000002");

        substitution.addSubstitute(substitute);

        assertThat(substitution.substitutes()).containsExactly(substitute);
    }

    @Test
    void assemblySubstituteIsAllowed() {
        CatalogItemReference item = assembly("00000000-0000-0000-0000-000000000003");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);
        CatalogItemReference substitute = assembly("00000000-0000-0000-0000-000000000004");

        substitution.addSubstitute(substitute);

        assertThat(substitution.substitutes()).containsExactly(substitute);
    }

    @Test
    void crossTypeRejected() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000005");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);

        assertThatThrownBy(() -> substitution.addSubstitute(
                assembly("00000000-0000-0000-0000-000000000006")))
                .isInstanceOf(InvalidSubstituteException.class);
    }

    @Test
    void selfSubstitutionRejected() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000007");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);

        assertThatThrownBy(() -> substitution.addSubstitute(item))
                .isInstanceOf(InvalidSubstituteException.class);
    }

    @Test
    void duplicateRejected() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000008");
        CatalogItemReference substitute = part("00000000-0000-0000-0000-000000000009");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);
        substitution.addSubstitute(substitute);

        assertThatThrownBy(() -> substitution.addSubstitute(substitute))
                .isInstanceOf(DuplicateSubstituteException.class);
    }

    @Test
    void nonPartAssemblyBaseRejected() {
        CatalogItemReference item = new CatalogItemReference(
                CatalogItemType.BUY, UUID.fromString("00000000-0000-0000-0000-000000000010"));
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);

        assertThatThrownBy(() -> substitution.addSubstitute(item))
                .isInstanceOf(InvalidSubstituteException.class);
    }

    @Test
    void removeSubstituteRemoves() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000011");
        CatalogItemReference substitute = part("00000000-0000-0000-0000-000000000012");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);
        substitution.addSubstitute(substitute);

        substitution.removeSubstitute(substitute);

        assertThat(substitution.substitutes()).isEmpty();
    }

    @Test
    void removeMissingSubstituteRejected() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000013");
        CatalogItemReference substitute = part("00000000-0000-0000-0000-000000000014");
        Substitution substitution = Substitution.create(OrganizationId.generate(), AssemblyId.generate(), item);

        assertThatThrownBy(() -> substitution.removeSubstitute(substitute))
                .isInstanceOf(SubstituteNotFoundException.class);
    }
}
