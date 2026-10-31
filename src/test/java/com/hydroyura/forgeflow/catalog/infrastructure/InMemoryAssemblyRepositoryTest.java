package com.hydroyura.forgeflow.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryAssemblyRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryAssemblyRepository repository = new InMemoryAssemblyRepository();

    private static Assembly assembly(OrganizationId org, String number, String name) {
        return Assembly.create(AssemblyId.generate(), number, name, org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveAssemblyOfOrganization() {
        Assembly assembly = assembly(orgA, "А-1", "Узел");
        repository.save(assembly);

        assertThat(repository.findById(assembly.getId(), orgA)).contains(assembly);
        assertThat(repository.findById(assembly.getId(), orgB)).isEmpty();

        assembly.markDeleted();
        repository.save(assembly);
        assertThat(repository.findById(assembly.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveAssembliesOfOrganizationSortedByNumber() {
        Assembly a2 = assembly(orgA, "А-2", "Вторая");
        Assembly a1 = assembly(orgA, "А-1", "Первая");
        Assembly other = assembly(orgB, "А-3", "Чужая");
        Assembly deleted = assembly(orgA, "А-0", "Удалённая");
        deleted.markDeleted();
        repository.save(a2);
        repository.save(a1);
        repository.save(other);
        repository.save(deleted);

        List<Assembly> result = repository.findAll(orgA);

        assertThat(result).extracting(Assembly::getNumber).containsExactly("А-1", "А-2");
    }

    @Test
    void existsByNumber_checksOnlyActiveAssembliesOfOrganization() {
        Assembly assembly = assembly(orgA, "А-1", "Узел");
        repository.save(assembly);

        assertThat(repository.existsByNumber("А-1", orgA)).isTrue();
        assertThat(repository.existsByNumber("А-1", orgB)).isFalse();
        assertThat(repository.existsByNumber("А-2", orgA)).isFalse();

        assembly.markDeleted();
        repository.save(assembly);
        assertThat(repository.existsByNumber("А-1", orgA)).isFalse();
    }
}
