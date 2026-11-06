package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.DuplicateTechnologyProcessNumberException;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import java.sql.SQLException;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JpaTechnologyProcessRepositoryAdapterTest {

    private final TechnologyProcessJpaRepository repository = mock(TechnologyProcessJpaRepository.class);
    private final TechnologyProcessOperationJpaRepository operationRepository =
            mock(TechnologyProcessOperationJpaRepository.class);
    private final JpaTechnologyProcessRepository adapter =
            new JpaTechnologyProcessRepository(repository, operationRepository);

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static TechnologyProcess process(String number) {
        return TechnologyProcess.create(
                TechnologyProcessId.generate(), number, "Фрезеровка",
                PartId.generate(), OrganizationId.generate(), NOW, NOW);
    }

    @Test
    void save_uniqueNumberConstraint_translatesToDomainException() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate"),
                        "technology_process_number_org_uq"));
        when(repository.save(any(TechnologyProcessEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(process("TP-1")))
                .isInstanceOf(DuplicateTechnologyProcessNumberException.class);
    }

    @Test
    void save_foreignConstraint_isNotMasked() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("fk violation",
                new ConstraintViolationException("fk violation", new SQLException("fk violation"),
                        "technology_process_operation_process_id_fkey"));
        when(repository.save(any(TechnologyProcessEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(process("TP-1")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateTechnologyProcessNumberException.class);
    }
}
