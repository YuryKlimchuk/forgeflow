package com.hydroyura.forgeflow.technology.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.infrastructure.InMemoryTechnologyProcessRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TechnologyProcessServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final PartId part = PartId.generate();
    private final PartId otherPart = PartId.generate();
    private final InMemoryTechnologyProcessRepository repository = new InMemoryTechnologyProcessRepository();
    private final TechnologyProcessService service = new TechnologyProcessService(repository);

    private TechnologyProcess process(OrganizationId org, PartId partId, String number) {
        return TechnologyProcess.create(
                TechnologyProcessId.generate(), number, "Процесс " + number, partId, org, NOW, NOW);
    }

    @Test
    void register_singleNonDefaultProcess_saves() {
        TechnologyProcess process = process(orgA, part, "TP-1");

        service.register(process);

        assertThat(repository.findById(process.getId(), orgA)).contains(process);
    }

    @Test
    void register_duplicateNumberInSameOrganization_rejectsAndDoesNotSave() {
        service.register(process(orgA, part, "TP-1"));
        TechnologyProcess duplicate = process(orgA, otherPart, "TP-1");

        assertThatThrownBy(() -> service.register(duplicate))
                .isInstanceOf(DuplicateTechnologyProcessNumberException.class);
        assertThat(repository.findById(duplicate.getId(), orgA)).isEmpty();
    }

    @Test
    void register_sameNumberInOtherOrganization_allowed() {
        service.register(process(orgA, part, "TP-1"));
        TechnologyProcess other = process(orgB, part, "TP-1");

        service.register(other);

        assertThat(repository.findById(other.getId(), orgB)).contains(other);
    }

    @Test
    void register_numberOfDeletedProcessInSameOrganization_allowed() {
        TechnologyProcess deleted = process(orgA, part, "TP-1");
        service.register(deleted);
        deleted.markDeleted();
        repository.save(deleted);

        TechnologyProcess replacement = process(orgA, part, "TP-1");

        service.register(replacement);

        assertThat(repository.findById(replacement.getId(), orgA)).contains(replacement);
    }

    @Test
    void register_secondNonDefaultProcessForPart_rejects() {
        service.register(process(orgA, part, "TP-1"));

        assertThatThrownBy(() -> service.register(process(orgA, part, "TP-2")))
                .isInstanceOf(InvalidDefaultProcessStateException.class);
    }

    @Test
    void registerThenDefaultThenSecondProcess_keepsInvariant() {
        TechnologyProcess first = process(orgA, part, "TP-1");
        service.register(first);
        service.markAsDefault(first.getId(), orgA);
        TechnologyProcess second = process(orgA, part, "TP-2");

        service.register(second);

        assertThat(first.isDefaultProcess()).isTrue();
        assertThat(second.isDefaultProcess()).isFalse();
        assertThat(repository.findByPartId(part, orgA)).hasSize(2);
    }

    @Test
    void markAsDefault_switchesDefaultWithoutTwoDefaults() {
        TechnologyProcess first = process(orgA, part, "TP-1");
        TechnologyProcess second = process(orgA, part, "TP-2");
        service.register(first);
        service.markAsDefault(first.getId(), orgA);
        service.register(second);

        service.markAsDefault(second.getId(), orgA);

        assertThat(first.isDefaultProcess()).isFalse();
        assertThat(second.isDefaultProcess()).isTrue();
        assertThat(repository.findByPartId(part, orgA))
                .filteredOn(TechnologyProcess::isDefaultProcess)
                .hasSize(1);
    }

    @Test
    void markAsDefault_missingProcess_rejects() {
        assertThatThrownBy(() -> service.markAsDefault(TechnologyProcessId.generate(), orgA))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void markAsDefault_processOfOtherOrganization_rejects() {
        TechnologyProcess process = process(orgA, part, "TP-1");
        service.register(process);

        assertThatThrownBy(() -> service.markAsDefault(process.getId(), orgB))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void unmarkDefault_soleDefaultWithAnotherActiveProcess_rejects() {
        TechnologyProcess first = process(orgA, part, "TP-1");
        service.register(first);
        service.markAsDefault(first.getId(), orgA);
        service.register(process(orgA, part, "TP-2"));

        assertThatThrownBy(() -> service.unmarkDefault(first.getId(), orgA))
                .isInstanceOf(InvalidDefaultProcessStateException.class);
        assertThat(first.isDefaultProcess()).isTrue();
    }

    @Test
    void unmarkDefault_singleProcess_allowed() {
        TechnologyProcess process = process(orgA, part, "TP-1");
        service.register(process);
        service.markAsDefault(process.getId(), orgA);

        service.unmarkDefault(process.getId(), orgA);

        assertThat(process.isDefaultProcess()).isFalse();
    }

    @Test
    void unmarkDefault_alreadyNotDefault_isNoOp() {
        TechnologyProcess process = process(orgA, part, "TP-1");
        service.register(process);
        Instant updatedAt = process.getUpdatedAt();

        service.unmarkDefault(process.getId(), orgA);

        assertThat(process.isDefaultProcess()).isFalse();
        assertThat(process.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void markAsDefault_alreadyDefault_isNoOp() {
        TechnologyProcess first = process(orgA, part, "TP-1");
        service.register(first);
        service.markAsDefault(first.getId(), orgA);
        TechnologyProcess second = process(orgA, part, "TP-2");
        service.register(second);

        Instant firstUpdatedAt = first.getUpdatedAt();
        Instant secondUpdatedAt = second.getUpdatedAt();
        service.markAsDefault(first.getId(), orgA);

        assertThat(first.isDefaultProcess()).isTrue();
        assertThat(second.isDefaultProcess()).isFalse();
        assertThat(first.getUpdatedAt()).isEqualTo(firstUpdatedAt);
        assertThat(second.getUpdatedAt()).isEqualTo(secondUpdatedAt);
    }

    @Test
    void defaultMutators_updateUpdatedAtWhenStateChanges() {
        TechnologyProcess process = process(orgA, part, "TP-1");
        service.register(process);
        Instant beforeDefault = process.getUpdatedAt();

        service.markAsDefault(process.getId(), orgA);
        assertThat(process.getUpdatedAt()).isAfter(beforeDefault);

        Instant afterDefault = process.getUpdatedAt();
        service.unmarkDefault(process.getId(), orgA);
        assertThat(process.getUpdatedAt()).isAfter(afterDefault);
    }
}
