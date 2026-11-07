package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * JPA-адаптер {@link SortamentRepository}.
 */
public class JpaSortamentRepository implements SortamentRepository {

    private static final String UNIQUE_NUMBER_CONSTRAINT = "sortament_number_org_uq";

    private final SortamentJpaRepository repository;
    private final SortamentMapper mapper = SortamentMapper.INSTANCE;

    public JpaSortamentRepository(SortamentJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Sortament sortament) {
        try {
            repository.save(mapper.toEntity(sortament));
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicateNumber(e, sortament.getNumber());
        }
    }

    @Override
    public Optional<Sortament> findById(SortamentId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId.value(), id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Sortament> findAll(OrganizationId organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(organizationId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return repository.existsByOrganizationIdAndNumberAndDeletedAtIsNull(organizationId.value(), number);
    }

    /**
     * Точечно транслирует нарушение частичного уникального индекса номера в
     * доменное исключение; прочие ограничения не маскируются.
     */
    private static RuntimeException translateDuplicateNumber(DataIntegrityViolationException e, String number) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && UNIQUE_NUMBER_CONSTRAINT.equals(violation.getConstraintName())) {
                return new DuplicateSortamentNumberException(number);
            }
            cause = cause.getCause();
        }
        return e;
    }
}
