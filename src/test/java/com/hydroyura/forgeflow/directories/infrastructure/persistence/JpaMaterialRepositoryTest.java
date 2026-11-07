package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.AbstractPostgresIntegrationTest;
import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
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
class JpaMaterialRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired
    MaterialRepository materialRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static Material material(OrganizationId org, String number, String name) {
        return Material.create(
                MaterialId.generate(), number, name, StandardType.GOST, "ГОСТ 1050-2013", "кг",
                org, CREATED, UPDATED);
    }

    @Test
    void roundTrip_preservesAllFields() {
        OrganizationId org = OrganizationId.generate();
        Material material = Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.DIN, "DIN EN 10088-2", "кг",
                org, CREATED, UPDATED);
        materialRepository.save(material);

        Material loaded = materialRepository.findById(material.getId(), org).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(material.getId());
        assertThat(loaded.getNumber()).isEqualTo("45");
        assertThat(loaded.getName()).isEqualTo("Сталь 45");
        assertThat(loaded.getStandardType()).isEqualTo(StandardType.DIN);
        assertThat(loaded.getStandard()).isEqualTo("DIN EN 10088-2");
        assertThat(loaded.getUnit()).isEqualTo("кг");
        assertThat(loaded.getOrganizationId()).isEqualTo(org);
        assertThat(loaded.getCreatedAt()).isEqualTo(CREATED);
        assertThat(loaded.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(loaded.getDeletedAt()).isNull();
    }

    @Test
    void reads_areTenantScoped() {
        OrganizationId orgA = OrganizationId.of(DEMO_ORGANIZATION_ID);
        OrganizationId orgB = OrganizationId.generate();
        Material material = material(orgA, "45", "Сталь 45");
        materialRepository.save(material);

        assertThat(materialRepository.findById(material.getId(), orgB)).isEmpty();
        assertThat(materialRepository.findAll(orgB)).isEmpty();
    }

    @Test
    void softDeletedMaterial_isExcludedFromReadsAndExistsByNumber() {
        OrganizationId org = OrganizationId.generate();
        Material material = material(org, "45", "Сталь 45");
        materialRepository.save(material);

        assertThat(materialRepository.findById(material.getId(), org)).isPresent();
        assertThat(materialRepository.existsByNumber("45", org)).isTrue();

        material.markDeleted();
        materialRepository.save(material);

        assertThat(materialRepository.findById(material.getId(), org)).isEmpty();
        assertThat(materialRepository.findAll(org)).isEmpty();
        assertThat(materialRepository.existsByNumber("45", org)).isFalse();
    }

    @Test
    void partialUniqueIndex_exists() {
        String indexdef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'material_number_org_uq'",
                String.class);

        assertThat(indexdef)
                .contains("organization_id")
                .contains("number")
                .contains("deleted_at IS NULL");
    }

    @Test
    void duplicateNumberAtDatabaseLevel_rejected() {
        OrganizationId org = OrganizationId.generate();

        insertMaterial(org, "45", UUID.randomUUID());

        assertThatThrownBy(() -> insertMaterial(org, "45", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void numberReleasedAfterSoftDelete() {
        OrganizationId org = OrganizationId.generate();
        Material first = material(org, "45", "Сталь 45");
        materialRepository.save(first);
        first.markDeleted();
        materialRepository.save(first);

        Material replacement = material(org, "45", "Сталь 45 новая");
        materialRepository.save(replacement);

        assertThat(materialRepository.findById(replacement.getId(), org)).isPresent();
        assertThat(materialRepository.existsByNumber("45", org)).isTrue();
    }

    @Test
    void findAll_sortedByNumber() {
        OrganizationId org = OrganizationId.generate();
        materialRepository.save(material(org, "2", "Второй"));
        materialRepository.save(material(org, "1", "Первый"));

        List<Material> result = materialRepository.findAll(org);

        assertThat(result).extracting(Material::getNumber).containsExactly("1", "2");
    }

    @Test
    void save_duplicateActiveNumber_translatesToDomainException() {
        OrganizationId org = OrganizationId.generate();
        materialRepository.save(material(org, "45", "Сталь 45"));

        Material duplicate = material(org, "45", "Сталь 45 дубль");

        assertThatThrownBy(() -> materialRepository.save(duplicate))
                .isInstanceOf(DuplicateMaterialNumberException.class);
    }

    @Test
    void standardType_isStoredAsString() {
        OrganizationId org = OrganizationId.generate();
        Material material = Material.create(
                MaterialId.generate(), "1.4301", "Нержавеющая сталь", StandardType.DIN, "DIN EN 10088-2", "кг",
                org, CREATED, CREATED);
        materialRepository.save(material);

        String stored = jdbcTemplate.queryForObject(
                "SELECT standard_type FROM material WHERE id = ?", String.class, material.getId().value());

        assertThat(stored).isEqualTo("DIN");
    }

    private void insertMaterial(OrganizationId org, String number, UUID id) {
        jdbcTemplate.update("""
                INSERT INTO material
                    (id, number, name, standard_type, standard, unit, organization_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, number, "Сталь", "GOST", "ГОСТ 1050-2013", "кг", org.value(),
                Timestamp.from(CREATED), Timestamp.from(CREATED));
    }
}
