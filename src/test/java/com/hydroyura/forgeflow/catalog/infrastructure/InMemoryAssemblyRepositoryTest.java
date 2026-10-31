package com.hydroyura.forgeflow.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryAssemblyRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryAssemblyRepository repository = new InMemoryAssemblyRepository();

    @Test
    void save_thenFind() {
        Assembly assembly = Assembly.create(
                AssemblyId.generate(), "А-1", "Корпус", OrganizationId.generate(), NOW, NOW);

        repository.save(assembly);

        assertThat(repository.findById(assembly.getId())).contains(assembly);
        assertThat(repository.findAll()).containsExactly(assembly);
    }

    @Test
    void delete_marksDeletedWithoutRemoving() {
        Assembly assembly = Assembly.create(
                AssemblyId.generate(), "А-1", "Корпус", OrganizationId.generate(), NOW, NOW);
        repository.save(assembly);

        repository.delete(assembly.getId());

        assertThat(repository.findById(assembly.getId())).contains(assembly);
        assertThat(repository.findById(assembly.getId()).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    void delete_unknownId_doesNothing() {
        repository.delete(AssemblyId.generate());

        assertThat(repository.findAll()).isEmpty();
    }
}
