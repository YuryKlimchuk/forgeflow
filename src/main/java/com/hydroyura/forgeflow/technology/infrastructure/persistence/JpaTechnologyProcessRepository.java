package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.DuplicateTechnologyProcessNumberException;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * JPA-адаптер {@link TechnologyProcessRepository}. Родитель и операции
 * маппятся вручную (по образцу {@code JpaPlanRepository}); набор операций
 * при сохранении полностью заменяется.
 */
public class JpaTechnologyProcessRepository implements TechnologyProcessRepository {

    private static final String UNIQUE_NUMBER_CONSTRAINT = "technology_process_number_org_uq";

    private final TechnologyProcessJpaRepository repository;
    private final TechnologyProcessOperationJpaRepository operationRepository;
    private final TechnologyMapper mapper = TechnologyMapper.INSTANCE;

    public JpaTechnologyProcessRepository(
            TechnologyProcessJpaRepository repository,
            TechnologyProcessOperationJpaRepository operationRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.operationRepository = Objects.requireNonNull(operationRepository, "operationRepository must not be null");
    }

    @Override
    public void save(TechnologyProcess process) {
        try {
            repository.save(mapper.toEntity(process));
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicateNumber(e, process.getNumber());
        }
        UUID processId = process.getId().value();
        operationRepository.deleteByProcessId(processId);
        operationRepository.flush();
        operationRepository.saveAll(mapper.toOperationEntities(process));
    }

    @Override
    public Optional<TechnologyProcess> findById(TechnologyProcessId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndId(organizationId.value(), id.value())
                .filter(entity -> entity.deletedAt == null)
                .map(entity -> mapper.toDomain(
                        entity, operationRepository.findByProcessIdOrderByOperationNumberAsc(id.value())));
    }

    @Override
    public List<TechnologyProcess> findByPartId(PartId partId, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndPartIdAndDeletedAtIsNullOrderByNumberAsc(
                        organizationId.value(), partId.value()).stream()
                .map(entity -> mapper.toDomain(
                        entity, operationRepository.findByProcessIdOrderByOperationNumberAsc(entity.id)))
                .toList();
    }

    @Override
    public List<TechnologyProcess> findAll(OrganizationId organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(organizationId.value()).stream()
                .map(entity -> mapper.toDomain(
                        entity, operationRepository.findByProcessIdOrderByOperationNumberAsc(entity.id)))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return repository.existsByOrganizationIdAndNumberAndDeletedAtIsNull(organizationId.value(), number);
    }

    /**
     * Точечно транслирует нарушение частичного уникального индекса номера в
     * доменное исключение; прочие ограничения (например, FK) не маскируются.
     */
    private static RuntimeException translateDuplicateNumber(DataIntegrityViolationException e, String number) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && UNIQUE_NUMBER_CONSTRAINT.equals(violation.getConstraintName())) {
                return new DuplicateTechnologyProcessNumberException(number);
            }
            cause = cause.getCause();
        }
        return e;
    }
}
