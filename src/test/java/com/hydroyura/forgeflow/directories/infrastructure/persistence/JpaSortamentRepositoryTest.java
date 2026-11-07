package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.AbstractPostgresIntegrationTest;
import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
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
class JpaSortamentRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired
    SortamentRepository sortamentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static Sortament sortament(OrganizationId org, String number, String name) {
        return Sortament.create(
                SortamentId.generate(), number, name, StandardType.GOST, "ГОСТ 2590-2006", "мм",
                org, CREATED, UPDATED);
    }

    @Test
    void roundTrip_preservesAllFields() {
        OrganizationId org = OrganizationId.generate();
        Sortament sortament = Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.DIN, "DIN EN 10210", "мм",
                org, CREATED, UPDATED);
        sortamentRepository.save(sortament);

        Sortament loaded = sortamentRepository.findById(sortament.getId(), org).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(sortament.getId());
        assertThat(loaded.getNumber()).isEqualTo("Круг В-30");
        assertThat(loaded.getName()).isEqualTo("Круг горячекатаный В-30");
        assertThat(loaded.getStandardType()).isEqualTo(StandardType.DIN);
        assertThat(loaded.getStandard()).isEqualTo("DIN EN 10210");
        assertThat(loaded.getUnit()).isEqualTo("мм");
        assertThat(loaded.getOrganizationId()).isEqualTo(org);
        assertThat(loaded.getCreatedAt()).isEqualTo(CREATED);
        assertThat(loaded.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(loaded.getDeletedAt()).isNull();
    }

    @Test
    void reads_areTenantScoped() {
        OrganizationId orgA = OrganizationId.of(DEMO_ORGANIZATION_ID);
        OrganizationId orgB = OrganizationId.generate();
        Sortament sortament = sortament(orgA, "Круг В-30", "Круг");
        sortamentRepository.save(sortament);

        assertThat(sortamentRepository.findById(sortament.getId(), orgB)).isEmpty();
        assertThat(sortamentRepository.findAll(orgB)).isEmpty();
    }

    @Test
    void softDeletedSortament_isExcludedFromReadsAndExistsByNumber() {
        OrganizationId org = OrganizationId.generate();
        Sortament sortament = sortament(org, "Круг В-30", "Круг");
        sortamentRepository.save(sortament);

        assertThat(sortamentRepository.findById(sortament.getId(), org)).isPresent();
        assertThat(sortamentRepository.existsByNumber("Круг В-30", org)).isTrue();

        sortament.markDeleted();
        sortamentRepository.save(sortament);

        assertThat(sortamentRepository.findById(sortament.getId(), org)).isEmpty();
        assertThat(sortamentRepository.findAll(org)).isEmpty();
        assertThat(sortamentRepository.existsByNumber("Круг В-30", org)).isFalse();
    }

    @Test
    void partialUniqueIndex_exists() {
        String indexdef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'sortament_number_org_uq'",
                String.class);

        assertThat(indexdef)
                .contains("organization_id")
                .contains("number")
                .contains("deleted_at IS NULL");
    }

    @Test
    void duplicateNumberAtDatabaseLevel_rejected() {
        OrganizationId org = OrganizationId.generate();

        insertSortament(org, "Круг В-30", UUID.randomUUID());

        assertThatThrownBy(() -> insertSortament(org, "Круг В-30", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void numberReleasedAfterSoftDelete() {
        OrganizationId org = OrganizationId.generate();
        Sortament first = sortament(org, "Круг В-30", "Круг");
        sortamentRepository.save(first);
        first.markDeleted();
        sortamentRepository.save(first);

        Sortament replacement = sortament(org, "Круг В-30", "Круг новый");
        sortamentRepository.save(replacement);

        assertThat(sortamentRepository.findById(replacement.getId(), org)).isPresent();
        assertThat(sortamentRepository.existsByNumber("Круг В-30", org)).isTrue();
    }

    @Test
    void findAll_sortedByNumber() {
        OrganizationId org = OrganizationId.generate();
        sortamentRepository.save(sortament(org, "Лист", "Лист"));
        sortamentRepository.save(sortament(org, "Круг", "Круг"));

        List<Sortament> result = sortamentRepository.findAll(org);

        assertThat(result).extracting(Sortament::getNumber).containsExactly("Круг", "Лист");
    }

    @Test
    void save_duplicateActiveNumber_translatesToDomainException() {
        OrganizationId org = OrganizationId.generate();
        sortamentRepository.save(sortament(org, "Круг В-30", "Круг"));

        Sortament duplicate = sortament(org, "Круг В-30", "Круг дубль");

        assertThatThrownBy(() -> sortamentRepository.save(duplicate))
                .isInstanceOf(DuplicateSortamentNumberException.class);
    }

    @Test
    void standardType_isStoredAsString() {
        OrganizationId org = OrganizationId.generate();
        Sortament sortament = Sortament.create(
                SortamentId.generate(), "Труба EN 10210", "Труба конструкционная", StandardType.DIN, "DIN EN 10210", "мм",
                org, CREATED, CREATED);
        sortamentRepository.save(sortament);

        String stored = jdbcTemplate.queryForObject(
                "SELECT standard_type FROM sortament WHERE id = ?", String.class, sortament.getId().value());

        assertThat(stored).isEqualTo("DIN");
    }

    private void insertSortament(OrganizationId org, String number, UUID id) {
        jdbcTemplate.update("""
                INSERT INTO sortament
                    (id, number, name, standard_type, standard, unit, organization_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, number, "Круг", "GOST", "ГОСТ 2590-2006", "мм", org.value(),
                Timestamp.from(CREATED), Timestamp.from(CREATED));
    }
}
