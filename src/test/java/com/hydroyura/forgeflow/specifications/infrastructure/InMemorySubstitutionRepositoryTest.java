package com.hydroyura.forgeflow.specifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.domain.Substitution;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemorySubstitutionRepositoryTest {

    @Test
    void saveAndFindById() {
        InMemorySubstitutionRepository repository = new InMemorySubstitutionRepository();
        OrganizationId organizationId = OrganizationId.generate();
        AssemblyId assemblyId = AssemblyId.generate();
        CatalogItemReference item = new CatalogItemReference(
                CatalogItemType.PART, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        Substitution substitution = Substitution.create(organizationId, assemblyId, item);
        substitution.addSubstitute(new CatalogItemReference(
                CatalogItemType.PART, UUID.fromString("00000000-0000-0000-0000-000000000002")));

        repository.save(substitution);

        assertThat(repository.findById(organizationId, assemblyId, item)).contains(substitution);
    }

    @Test
    void deleteRemoves() {
        InMemorySubstitutionRepository repository = new InMemorySubstitutionRepository();
        OrganizationId organizationId = OrganizationId.generate();
        AssemblyId assemblyId = AssemblyId.generate();
        CatalogItemReference item = new CatalogItemReference(
                CatalogItemType.PART, UUID.fromString("00000000-0000-0000-0000-000000000003"));
        repository.save(Substitution.create(organizationId, assemblyId, item));

        repository.delete(organizationId, assemblyId, item);

        assertThat(repository.findById(organizationId, assemblyId, item)).isEmpty();
    }

    @Test
    void tenantIsolationBetweenOrganizations() {
        InMemorySubstitutionRepository repository = new InMemorySubstitutionRepository();
        OrganizationId organizationA = OrganizationId.generate();
        OrganizationId organizationB = OrganizationId.generate();
        AssemblyId assemblyId = AssemblyId.generate();
        CatalogItemReference item = new CatalogItemReference(
                CatalogItemType.PART, UUID.fromString("00000000-0000-0000-0000-000000000004"));
        repository.save(Substitution.create(organizationA, assemblyId, item));

        assertThat(repository.findById(organizationA, assemblyId, item)).isPresent();
        assertThat(repository.findById(organizationB, assemblyId, item)).isEmpty();
    }
}
