package com.hydroyura.forgeflow.technology.infrastructure;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link EquipmentRepository} для тестов.
 *
 * <p>Чтение фильтрует по организации и исключает удалённые записи; списки
 * сортируются по номеру.</p>
 */
public class InMemoryEquipmentRepository implements EquipmentRepository {

    private final Map<EquipmentId, Equipment> equipment = new ConcurrentHashMap<>();

    @Override
    public void save(Equipment value) {
        equipment.put(value.getId(), value);
    }

    @Override
    public Optional<Equipment> findById(EquipmentId id, OrganizationId organizationId) {
        Equipment value = equipment.get(id);
        if (value == null || !value.getOrganizationId().equals(organizationId) || value.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @Override
    public List<Equipment> findAll(OrganizationId organizationId) {
        return equipment.values().stream()
                .filter(value -> value.getOrganizationId().equals(organizationId))
                .filter(value -> value.getDeletedAt() == null)
                .sorted(Comparator.comparing(Equipment::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return equipment.values().stream()
                .anyMatch(value -> value.getOrganizationId().equals(organizationId)
                        && value.getNumber().equals(number)
                        && value.getDeletedAt() == null);
    }
}
