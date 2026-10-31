package com.hydroyura.forgeflow.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryVzkRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryVzkRepository repository = new InMemoryVzkRepository();

    private static Vzk vzk(OrganizationId org, String number, String name) {
        return Vzk.create(VzkId.generate(), number, name, org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveVzkOfOrganization() {
        Vzk vzk = vzk(orgA, "В-1", "Группа");
        repository.save(vzk);

        assertThat(repository.findById(vzk.getId(), orgA)).contains(vzk);
        assertThat(repository.findById(vzk.getId(), orgB)).isEmpty();

        vzk.markDeleted();
        repository.save(vzk);
        assertThat(repository.findById(vzk.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveVzksOfOrganizationSortedByNumber() {
        Vzk v2 = vzk(orgA, "В-2", "Вторая");
        Vzk v1 = vzk(orgA, "В-1", "Первая");
        Vzk other = vzk(orgB, "В-3", "Чужая");
        Vzk deleted = vzk(orgA, "В-0", "Удалённая");
        deleted.markDeleted();
        repository.save(v2);
        repository.save(v1);
        repository.save(other);
        repository.save(deleted);

        List<Vzk> result = repository.findAll(orgA);

        assertThat(result).extracting(Vzk::getNumber).containsExactly("В-1", "В-2");
    }

    @Test
    void existsByNumber_checksOnlyActiveVzksOfOrganization() {
        Vzk vzk = vzk(orgA, "В-1", "Группа");
        repository.save(vzk);

        assertThat(repository.existsByNumber("В-1", orgA)).isTrue();
        assertThat(repository.existsByNumber("В-1", orgB)).isFalse();
        assertThat(repository.existsByNumber("В-2", orgA)).isFalse();

        vzk.markDeleted();
        repository.save(vzk);
        assertThat(repository.existsByNumber("В-1", orgA)).isFalse();
    }
}
