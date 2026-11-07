package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * JPA-адаптер {@link MaterialRepository}.
 */
public class JpaMaterialRepository implements MaterialRepository {

    private static final String UNIQUE_NUMBER_CONSTRAINT = "material_number_org_uq";

    private final MaterialJpaRepository repository;
    private final MaterialMapper mapper = MaterialMapper.INSTANCE;

    public JpaMaterialRepository(MaterialJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Material material) {
        try {
            repository.save(mapper.toEntity(material));
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicateNumber(e, material.getNumber());
        }
    }

    @Override
    public Optional<Material> findById(MaterialId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId.value(), id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Material> findAll(OrganizationId organizationId) {
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
                return new DuplicateMaterialNumberException(number);
            }
            cause = cause.getCause();
        }
        return e;
    }
}
