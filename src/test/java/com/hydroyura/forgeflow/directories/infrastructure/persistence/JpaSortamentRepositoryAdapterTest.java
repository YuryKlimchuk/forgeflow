package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.sql.SQLException;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JpaSortamentRepositoryAdapterTest {

    private final SortamentJpaRepository repository = mock(SortamentJpaRepository.class);
    private final JpaSortamentRepository adapter = new JpaSortamentRepository(repository);

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static Sortament sortament(String number) {
        return Sortament.create(
                SortamentId.generate(), number, "Круг", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), NOW, NOW);
    }

    @Test
    void save_uniqueNumberConstraint_translatesToDomainException() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate"),
                        "sortament_number_org_uq"));
        when(repository.save(any(SortamentEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(sortament("Круг В-30")))
                .isInstanceOf(DuplicateSortamentNumberException.class);
    }

    @Test
    void save_foreignConstraint_isNotMasked() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("fk violation",
                new ConstraintViolationException("fk violation", new SQLException("fk violation"),
                        "sortament_organization_id_fkey"));
        when(repository.save(any(SortamentEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(sortament("Круг В-30")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateSortamentNumberException.class);
    }
}
