package com.hydroyura.forgeflow.technology.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryTechnologyProcessRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final PartId part = PartId.generate();
    private final PartId otherPart = PartId.generate();
    private final InMemoryTechnologyProcessRepository repository = new InMemoryTechnologyProcessRepository();

    private static TechnologyProcess process(OrganizationId org, PartId partId, String number) {
        return TechnologyProcess.create(
                TechnologyProcessId.generate(), number, "Процесс " + number, partId, org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveProcessOfOrganization() {
        TechnologyProcess process = process(orgA, part, "TP-1");
        repository.save(process);

        assertThat(repository.findById(process.getId(), orgA)).contains(process);
        assertThat(repository.findById(process.getId(), orgB)).isEmpty();

        process.markDeleted();
        repository.save(process);
        assertThat(repository.findById(process.getId(), orgA)).isEmpty();
    }

    @Test
    void findByPartId_returnsOnlyActiveProcessesOfPartAndOrganization() {
        TechnologyProcess own = process(orgA, part, "TP-1");
        TechnologyProcess otherPartProcess = process(orgA, otherPart, "TP-2");
        TechnologyProcess otherOrgProcess = process(orgB, part, "TP-3");
        TechnologyProcess deleted = process(orgA, part, "TP-4");
        deleted.markDeleted();
        repository.save(own);
        repository.save(otherPartProcess);
        repository.save(otherOrgProcess);
        repository.save(deleted);

        List<TechnologyProcess> result = repository.findByPartId(part, orgA);

        assertThat(result).containsExactly(own);
    }

    @Test
    void findAll_returnsActiveProcessesOfOrganizationSortedByNumber() {
        TechnologyProcess second = process(orgA, part, "TP-2");
        TechnologyProcess first = process(orgA, part, "TP-1");
        TechnologyProcess otherOrgProcess = process(orgB, part, "TP-3");
        TechnologyProcess deleted = process(orgA, part, "TP-0");
        deleted.markDeleted();
        repository.save(second);
        repository.save(first);
        repository.save(otherOrgProcess);
        repository.save(deleted);

        List<TechnologyProcess> result = repository.findAll(orgA);

        assertThat(result).extracting(TechnologyProcess::getNumber).containsExactly("TP-1", "TP-2");
    }

    @Test
    void existsByNumber_checksOnlyActiveProcessesOfOrganization() {
        TechnologyProcess process = process(orgA, part, "TP-1");
        repository.save(process);

        assertThat(repository.existsByNumber("TP-1", orgA)).isTrue();
        assertThat(repository.existsByNumber("TP-1", orgB)).isFalse();
        assertThat(repository.existsByNumber("TP-2", orgA)).isFalse();

        process.markDeleted();
        repository.save(process);
        assertThat(repository.existsByNumber("TP-1", orgA)).isFalse();
    }
}
