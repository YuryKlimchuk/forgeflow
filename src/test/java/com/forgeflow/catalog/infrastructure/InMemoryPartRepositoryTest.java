package com.forgeflow.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgeflow.catalog.api.PartId;
import com.forgeflow.catalog.domain.part.Part;
import com.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryPartRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryPartRepository repository = new InMemoryPartRepository();

    private static Part part(OrganizationId org, String number, String name) {
        return Part.create(PartId.generate(), number, name, org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActivePartOfOrganization() {
        Part part = part(orgA, "P-1", "Деталь");
        repository.save(part);

        assertThat(repository.findById(part.getId(), orgA)).contains(part);
        assertThat(repository.findById(part.getId(), orgB)).isEmpty();

        part.markDeleted();
        repository.save(part);
        assertThat(repository.findById(part.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActivePartsOfOrganizationSortedByNumber() {
        Part p2 = part(orgA, "P-2", "Вторая");
        Part p1 = part(orgA, "P-1", "Первая");
        Part other = part(orgB, "P-3", "Чужая");
        Part deleted = part(orgA, "P-0", "Удалённая");
        deleted.markDeleted();
        repository.save(p2);
        repository.save(p1);
        repository.save(other);
        repository.save(deleted);

        List<Part> result = repository.findAll(orgA);

        assertThat(result).extracting(Part::getNumber).containsExactly("P-1", "P-2");
    }

    @Test
    void existsByNumber_checksOnlyActivePartsOfOrganization() {
        Part part = part(orgA, "P-1", "Деталь");
        repository.save(part);

        assertThat(repository.existsByNumber("P-1", orgA)).isTrue();
        assertThat(repository.existsByNumber("P-1", orgB)).isFalse();
        assertThat(repository.existsByNumber("P-2", orgA)).isFalse();

        part.markDeleted();
        repository.save(part);
        assertThat(repository.existsByNumber("P-1", orgA)).isFalse();
    }
}
