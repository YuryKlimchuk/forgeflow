package com.hydroyura.forgeflow.technology.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryEquipmentRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryEquipmentRepository repository = new InMemoryEquipmentRepository();

    private static Equipment equipment(OrganizationId org, String number, String name) {
        return Equipment.create(EquipmentId.generate(), number, name, "Токарный", org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveEquipmentOfOrganization() {
        Equipment equipment = equipment(orgA, "С-001", "Станок");
        repository.save(equipment);

        assertThat(repository.findById(equipment.getId(), orgA)).contains(equipment);
        assertThat(repository.findById(equipment.getId(), orgB)).isEmpty();

        equipment.markDeleted();
        repository.save(equipment);
        assertThat(repository.findById(equipment.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveEquipmentOfOrganizationSortedByNumber() {
        Equipment second = equipment(orgA, "2", "Второй");
        Equipment first = equipment(orgA, "1", "Первый");
        Equipment other = equipment(orgB, "3", "Чужой");
        Equipment deleted = equipment(orgA, "0", "Удалённый");
        deleted.markDeleted();
        repository.save(second);
        repository.save(first);
        repository.save(other);
        repository.save(deleted);

        List<Equipment> result = repository.findAll(orgA);

        assertThat(result).extracting(Equipment::getNumber).containsExactly("1", "2");
    }

    @Test
    void existsByNumber_checksOnlyActiveEquipmentOfOrganization() {
        Equipment equipment = equipment(orgA, "С-001", "Станок");
        repository.save(equipment);

        assertThat(repository.existsByNumber("С-001", orgA)).isTrue();
        assertThat(repository.existsByNumber("С-001", orgB)).isFalse();
        assertThat(repository.existsByNumber("С-999", orgA)).isFalse();

        equipment.markDeleted();
        repository.save(equipment);
        assertThat(repository.existsByNumber("С-001", orgA)).isFalse();
    }
}
