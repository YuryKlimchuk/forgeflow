package com.hydroyura.forgeflow.specifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import org.junit.jupiter.api.Test;

class InMemorySpecificationRepositoryTest {

    @Test
    void saveAndFindById() {
        InMemorySpecificationRepository repository = new InMemorySpecificationRepository();
        OrganizationId orgId = OrganizationId.generate();
        AssemblyId assemblyId = AssemblyId.generate();
        Specification specification = Specification.create(orgId, assemblyId);

        repository.save(specification);

        assertThat(repository.findById(orgId, assemblyId)).contains(specification);
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        InMemorySpecificationRepository repository = new InMemorySpecificationRepository();

        assertThat(repository.findById(OrganizationId.generate(), AssemblyId.generate())).isEmpty();
    }

    @Test
    void tenantIsolation() {
        InMemorySpecificationRepository repository = new InMemorySpecificationRepository();
        OrganizationId orgA = OrganizationId.generate();
        OrganizationId orgB = OrganizationId.generate();
        AssemblyId assemblyId = AssemblyId.generate();
        Specification specificationA = Specification.create(orgA, assemblyId);
        repository.save(specificationA);

        assertThat(repository.findById(orgA, assemblyId)).contains(specificationA);
        assertThat(repository.findById(orgB, assemblyId)).isEmpty();
    }
}
