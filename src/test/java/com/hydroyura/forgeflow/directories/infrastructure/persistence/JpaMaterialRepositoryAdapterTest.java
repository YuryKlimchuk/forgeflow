package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.sql.SQLException;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JpaMaterialRepositoryAdapterTest {

    private final MaterialJpaRepository repository = mock(MaterialJpaRepository.class);
    private final JpaMaterialRepository adapter = new JpaMaterialRepository(repository);

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static Material material(String number) {
        return Material.create(
                MaterialId.generate(), number, "Сталь", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), NOW, NOW);
    }

    @Test
    void save_uniqueNumberConstraint_translatesToDomainException() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate"),
                        "material_number_org_uq"));
        when(repository.save(any(MaterialEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(material("45")))
                .isInstanceOf(DuplicateMaterialNumberException.class);
    }

    @Test
    void save_foreignConstraint_isNotMasked() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("fk violation",
                new ConstraintViolationException("fk violation", new SQLException("fk violation"),
                        "material_organization_id_fkey"));
        when(repository.save(any(MaterialEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(material("45")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateMaterialNumberException.class);
    }
}
