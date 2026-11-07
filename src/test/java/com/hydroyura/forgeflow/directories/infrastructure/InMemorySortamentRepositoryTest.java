package com.hydroyura.forgeflow.directories.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemorySortamentRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemorySortamentRepository repository = new InMemorySortamentRepository();

    private static Sortament sortament(OrganizationId org, String number, String name) {
        return Sortament.create(SortamentId.generate(), number, name, StandardType.GOST, "ГОСТ 2590-2006", "мм",
                org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveSortamentOfOrganization() {
        Sortament sortament = sortament(orgA, "Круг В-30", "Круг");
        repository.save(sortament);

        assertThat(repository.findById(sortament.getId(), orgA)).contains(sortament);
        assertThat(repository.findById(sortament.getId(), orgB)).isEmpty();

        sortament.markDeleted();
        repository.save(sortament);
        assertThat(repository.findById(sortament.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveSortamentsOfOrganizationSortedByNumber() {
        Sortament second = sortament(orgA, "Лист", "Лист");
        Sortament first = sortament(orgA, "Круг", "Круг");
        Sortament other = sortament(orgB, "Труба", "Чужой");
        Sortament deleted = sortament(orgA, "Уголок", "Удалённый");
        deleted.markDeleted();
        repository.save(second);
        repository.save(first);
        repository.save(other);
        repository.save(deleted);

        List<Sortament> result = repository.findAll(orgA);

        assertThat(result).extracting(Sortament::getNumber).containsExactly("Круг", "Лист");
    }

    @Test
    void existsByNumber_checksOnlyActiveSortamentsOfOrganization() {
        Sortament sortament = sortament(orgA, "Круг В-30", "Круг");
        repository.save(sortament);

        assertThat(repository.existsByNumber("Круг В-30", orgA)).isTrue();
        assertThat(repository.existsByNumber("Круг В-30", orgB)).isFalse();
        assertThat(repository.existsByNumber("Нет", orgA)).isFalse();

        sortament.markDeleted();
        repository.save(sortament);
        assertThat(repository.existsByNumber("Круг В-30", orgA)).isFalse();
    }
}
