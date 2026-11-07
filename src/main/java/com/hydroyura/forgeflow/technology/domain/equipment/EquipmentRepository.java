package com.hydroyura.forgeflow.technology.domain.equipment;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения оборудования.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * единицы оборудования не участвуют в чтении.</p>
 */
public interface EquipmentRepository {

    void save(Equipment equipment);

    Optional<Equipment> findById(EquipmentId id, OrganizationId organizationId);

    List<Equipment> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
