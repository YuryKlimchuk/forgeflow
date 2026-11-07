package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * JPA-адаптер {@link EquipmentRepository}.
 */
public class JpaEquipmentRepository implements EquipmentRepository {

    private static final String UNIQUE_NUMBER_CONSTRAINT = "equipment_number_org_uq";

    private final EquipmentJpaRepository repository;
    private final EquipmentMapper mapper = EquipmentMapper.INSTANCE;

    public JpaEquipmentRepository(EquipmentJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Equipment equipment) {
        try {
            repository.save(mapper.toEntity(equipment));
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicateNumber(e, equipment.getNumber());
        }
    }

    @Override
    public Optional<Equipment> findById(EquipmentId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId.value(), id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Equipment> findAll(OrganizationId organizationId) {
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
                return new DuplicateEquipmentNumberException(number);
            }
            cause = cause.getCause();
        }
        return e;
    }
}
