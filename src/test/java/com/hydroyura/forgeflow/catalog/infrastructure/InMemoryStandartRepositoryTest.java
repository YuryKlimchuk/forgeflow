package com.hydroyura.forgeflow.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryStandartRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryStandartRepository repository = new InMemoryStandartRepository();

    private static Standart standart(OrganizationId org, String number, String name) {
        return Standart.create(StandartId.generate(), number, name, org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveStandartOfOrganization() {
        Standart standart = standart(orgA, "С-1", "Стандарт");
        repository.save(standart);

        assertThat(repository.findById(standart.getId(), orgA)).contains(standart);
        assertThat(repository.findById(standart.getId(), orgB)).isEmpty();

        standart.markDeleted();
        repository.save(standart);
        assertThat(repository.findById(standart.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveStandartsOfOrganizationSortedByNumber() {
        Standart s2 = standart(orgA, "С-2", "Второй");
        Standart s1 = standart(orgA, "С-1", "Первый");
        Standart other = standart(orgB, "С-3", "Чужой");
        Standart deleted = standart(orgA, "С-0", "Удалённый");
        deleted.markDeleted();
        repository.save(s2);
        repository.save(s1);
        repository.save(other);
        repository.save(deleted);

        List<Standart> result = repository.findAll(orgA);

        assertThat(result).extracting(Standart::getNumber).containsExactly("С-1", "С-2");
    }

    @Test
    void existsByNumber_checksOnlyActiveStandartsOfOrganization() {
        Standart standart = standart(orgA, "С-1", "Стандарт");
        repository.save(standart);

        assertThat(repository.existsByNumber("С-1", orgA)).isTrue();
        assertThat(repository.existsByNumber("С-1", orgB)).isFalse();
        assertThat(repository.existsByNumber("С-2", orgA)).isFalse();

        standart.markDeleted();
        repository.save(standart);
        assertThat(repository.existsByNumber("С-1", orgA)).isFalse();
    }
}
