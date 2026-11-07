package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import java.sql.SQLException;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JpaEquipmentRepositoryAdapterTest {

    private final EquipmentJpaRepository repository = mock(EquipmentJpaRepository.class);
    private final JpaEquipmentRepository adapter = new JpaEquipmentRepository(repository);

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static Equipment equipment(String number) {
        return Equipment.create(
                EquipmentId.generate(), number, "Станок", "Токарный",
                OrganizationId.generate(), NOW, NOW);
    }

    @Test
    void save_uniqueNumberConstraint_translatesToDomainException() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate"),
                        "equipment_number_org_uq"));
        when(repository.save(any(EquipmentEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(equipment("С-001")))
                .isInstanceOf(DuplicateEquipmentNumberException.class);
    }

    @Test
    void save_foreignConstraint_isNotMasked() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("fk violation",
                new ConstraintViolationException("fk violation", new SQLException("fk violation"),
                        "equipment_organization_id_fkey"));
        when(repository.save(any(EquipmentEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(equipment("С-001")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateEquipmentNumberException.class);
    }
}
