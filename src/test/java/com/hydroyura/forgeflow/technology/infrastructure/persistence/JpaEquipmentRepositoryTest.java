package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.AbstractPostgresIntegrationTest;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
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
class JpaEquipmentRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired
    EquipmentRepository equipmentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static Equipment equipment(OrganizationId org, String number, String name) {
        return Equipment.create(
                EquipmentId.generate(), number, name, "Токарный", org, CREATED, UPDATED);
    }

    @Test
    void roundTrip_preservesAllFields() {
        OrganizationId org = OrganizationId.generate();
        Equipment equipment = Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "Фрезерный",
                org, CREATED, UPDATED);
        equipmentRepository.save(equipment);

        Equipment loaded = equipmentRepository.findById(equipment.getId(), org).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(equipment.getId());
        assertThat(loaded.getNumber()).isEqualTo("С-001");
        assertThat(loaded.getName()).isEqualTo("Станок токарный");
        assertThat(loaded.getType()).isEqualTo("Фрезерный");
        assertThat(loaded.getOrganizationId()).isEqualTo(org);
        assertThat(loaded.getCreatedAt()).isEqualTo(CREATED);
        assertThat(loaded.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(loaded.getDeletedAt()).isNull();
    }

    @Test
    void reads_areTenantScoped() {
        OrganizationId orgA = OrganizationId.of(DEMO_ORGANIZATION_ID);
        OrganizationId orgB = OrganizationId.generate();
        Equipment equipment = equipment(orgA, "С-001", "Станок");
        equipmentRepository.save(equipment);

        assertThat(equipmentRepository.findById(equipment.getId(), orgB)).isEmpty();
        assertThat(equipmentRepository.findAll(orgB)).isEmpty();
    }

    @Test
    void softDeletedEquipment_isExcludedFromReadsAndExistsByNumber() {
        OrganizationId org = OrganizationId.generate();
        Equipment equipment = equipment(org, "С-001", "Станок");
        equipmentRepository.save(equipment);

        assertThat(equipmentRepository.findById(equipment.getId(), org)).isPresent();
        assertThat(equipmentRepository.existsByNumber("С-001", org)).isTrue();

        equipment.markDeleted();
        equipmentRepository.save(equipment);

        assertThat(equipmentRepository.findById(equipment.getId(), org)).isEmpty();
        assertThat(equipmentRepository.findAll(org)).isEmpty();
        assertThat(equipmentRepository.existsByNumber("С-001", org)).isFalse();
    }

    @Test
    void partialUniqueIndex_exists() {
        String indexdef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'equipment_number_org_uq'",
                String.class);

        assertThat(indexdef)
                .contains("organization_id")
                .contains("number")
                .contains("deleted_at IS NULL");
    }

    @Test
    void duplicateNumberAtDatabaseLevel_rejected() {
        OrganizationId org = OrganizationId.generate();

        insertEquipment(org, "С-DUP", UUID.randomUUID());

        assertThatThrownBy(() -> insertEquipment(org, "С-DUP", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void numberReleasedAfterSoftDelete() {
        OrganizationId org = OrganizationId.generate();
        Equipment first = equipment(org, "С-REUSE", "Станок");
        equipmentRepository.save(first);
        first.markDeleted();
        equipmentRepository.save(first);

        Equipment replacement = equipment(org, "С-REUSE", "Новый станок");
        equipmentRepository.save(replacement);

        assertThat(equipmentRepository.findById(replacement.getId(), org)).isPresent();
        assertThat(equipmentRepository.existsByNumber("С-REUSE", org)).isTrue();
    }

    @Test
    void findAll_sortedByNumber() {
        OrganizationId org = OrganizationId.generate();
        equipmentRepository.save(equipment(org, "2", "Второй"));
        equipmentRepository.save(equipment(org, "1", "Первый"));

        List<Equipment> result = equipmentRepository.findAll(org);

        assertThat(result).extracting(Equipment::getNumber).containsExactly("1", "2");
    }

    @Test
    void save_duplicateActiveNumber_translatesToDomainException() {
        OrganizationId org = OrganizationId.generate();
        equipmentRepository.save(equipment(org, "С-001", "Станок"));

        Equipment duplicate = equipment(org, "С-001", "Дубль");

        assertThatThrownBy(() -> equipmentRepository.save(duplicate))
                .isInstanceOf(DuplicateEquipmentNumberException.class);
    }

    private void insertEquipment(OrganizationId org, String number, UUID id) {
        jdbcTemplate.update("""
                INSERT INTO equipment
                    (id, number, name, type, organization_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id, number, "Станок", "Токарный", org.value(),
                Timestamp.from(CREATED), Timestamp.from(CREATED));
    }
}
