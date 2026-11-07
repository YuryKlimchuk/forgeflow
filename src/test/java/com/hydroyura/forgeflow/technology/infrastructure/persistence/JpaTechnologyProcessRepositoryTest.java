package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.AbstractPostgresIntegrationTest;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.DuplicateTechnologyProcessNumberException;
import com.hydroyura.forgeflow.technology.domain.Operation;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class JpaTechnologyProcessRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired
    TechnologyProcessRepository technologyProcessRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void roundTrip_preservesProcessAndOrderedOperations() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();
        TechnologyProcess process = TechnologyProcess.restore(
                TechnologyProcessId.generate(), "TP-1", "Фрезеровка", part, org, true,
                CREATED, UPDATED, null,
                List.of(new Operation(20, "Б"), new Operation(10, "А")));
        technologyProcessRepository.save(process);

        TechnologyProcess loaded = technologyProcessRepository.findById(process.getId(), org).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(process.getId());
        assertThat(loaded.getNumber()).isEqualTo("TP-1");
        assertThat(loaded.getName()).isEqualTo("Фрезеровка");
        assertThat(loaded.getPartId()).isEqualTo(part);
        assertThat(loaded.getOrganizationId()).isEqualTo(org);
        assertThat(loaded.isDefaultProcess()).isTrue();
        assertThat(loaded.getCreatedAt()).isEqualTo(CREATED);
        assertThat(loaded.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(loaded.getDeletedAt()).isNull();
        assertThat(loaded.operations()).extracting(Operation::number).containsExactly(10, 20);
    }

    @Test
    void roundTrip_preservesNullableOperationEquipmentId() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();
        EquipmentId equipmentId = EquipmentId.generate();
        TechnologyProcess process = TechnologyProcess.restore(
                TechnologyProcessId.generate(), "TP-1", "Фрезеровка", part, org, false,
                CREATED, UPDATED, null,
                List.of(
                        new Operation(20, "Б"),
                        new Operation(10, "А", equipmentId)));
        technologyProcessRepository.save(process);

        TechnologyProcess loaded = technologyProcessRepository.findById(process.getId(), org).orElseThrow();

        assertThat(loaded.operations())
                .extracting(Operation::equipmentId)
                .containsExactly(equipmentId, null);
    }

    @Test
    void reads_areTenantScoped() {
        OrganizationId orgA = OrganizationId.of(DEMO_ORGANIZATION_ID);
        OrganizationId orgB = OrganizationId.generate();
        PartId part = PartId.generate();
        TechnologyProcess process = TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-1", "Фрезеровка", part, orgA, CREATED, CREATED);
        technologyProcessRepository.save(process);

        assertThat(technologyProcessRepository.findById(process.getId(), orgB)).isEmpty();
        assertThat(technologyProcessRepository.findAll(orgB)).isEmpty();
        assertThat(technologyProcessRepository.findByPartId(part, orgB)).isEmpty();
    }

    @Test
    void softDeletedProcess_isExcludedFromReadsAndExistsByNumber() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();
        TechnologyProcess process = TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-1", "Фрезеровка", part, org, CREATED, CREATED);
        technologyProcessRepository.save(process);

        assertThat(technologyProcessRepository.findById(process.getId(), org)).isPresent();
        assertThat(technologyProcessRepository.existsByNumber("TP-1", org)).isTrue();

        process.markDeleted();
        technologyProcessRepository.save(process);

        assertThat(technologyProcessRepository.findById(process.getId(), org)).isEmpty();
        assertThat(technologyProcessRepository.findAll(org)).isEmpty();
        assertThat(technologyProcessRepository.existsByNumber("TP-1", org)).isFalse();
    }

    @Test
    void partialUniqueIndex_exists() {
        String indexdef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'technology_process_number_org_uq'",
                String.class);

        assertThat(indexdef)
                .contains("organization_id")
                .contains("number")
                .contains("deleted_at IS NULL");
    }

    @Test
    void duplicateNumberAtDatabaseLevel_rejected() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();

        insertProcess(org, part, "TP-DUP", UUID.randomUUID());

        assertThatThrownBy(() -> insertProcess(org, part, "TP-DUP", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void numberReleasedAfterSoftDelete() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();
        TechnologyProcess first = TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-REUSE", "Фрезеровка", part, org, CREATED, CREATED);
        technologyProcessRepository.save(first);
        first.markDeleted();
        technologyProcessRepository.save(first);

        TechnologyProcess replacement = TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-REUSE", "Токарная", part, org, CREATED, CREATED);
        technologyProcessRepository.save(replacement);

        assertThat(technologyProcessRepository.findById(replacement.getId(), org)).isPresent();
        assertThat(technologyProcessRepository.existsByNumber("TP-REUSE", org)).isTrue();
    }

    @Test
    void findByPartId_sortedByNumber() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();
        technologyProcessRepository.save(TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-2", "Второй", part, org, CREATED, CREATED));
        technologyProcessRepository.save(TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-1", "Первый", part, org, CREATED, CREATED));

        List<TechnologyProcess> result = technologyProcessRepository.findByPartId(part, org);

        assertThat(result).extracting(TechnologyProcess::getNumber).containsExactly("TP-1", "TP-2");
    }

    @Test
    void save_duplicateActiveNumber_translatesToDomainException() {
        OrganizationId org = OrganizationId.generate();
        PartId part = PartId.generate();
        technologyProcessRepository.save(TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-DUP", "Первый", part, org, CREATED, CREATED));

        TechnologyProcess duplicate = TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-DUP", "Второй", part, org, CREATED, CREATED);

        assertThatThrownBy(() -> technologyProcessRepository.save(duplicate))
                .isInstanceOf(DuplicateTechnologyProcessNumberException.class);
    }

    private void insertProcess(OrganizationId org, PartId part, String number, UUID id) {
        jdbcTemplate.update("""
                INSERT INTO technology_process
                    (id, number, name, part_id, organization_id, default_process, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, number, "Фрезеровка", part.value(), org.value(), false,
                Timestamp.from(CREATED), Timestamp.from(CREATED));
    }
}
