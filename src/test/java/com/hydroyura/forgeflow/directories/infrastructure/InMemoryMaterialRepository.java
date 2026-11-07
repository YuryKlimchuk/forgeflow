package com.hydroyura.forgeflow.directories.infrastructure;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link MaterialRepository} для тестов.
 */
public class InMemoryMaterialRepository implements MaterialRepository {

    private final Map<MaterialId, Material> materials = new ConcurrentHashMap<>();

    @Override
    public void save(Material material) {
        materials.put(material.getId(), material);
    }

    @Override
    public Optional<Material> findById(MaterialId id, OrganizationId organizationId) {
        Material material = materials.get(id);
        if (material == null || !material.getOrganizationId().equals(organizationId) || material.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(material);
    }

    @Override
    public List<Material> findAll(OrganizationId organizationId) {
        return materials.values().stream()
                .filter(material -> material.getOrganizationId().equals(organizationId))
                .filter(material -> material.getDeletedAt() == null)
                .sorted(Comparator.comparing(Material::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return materials.values().stream()
                .anyMatch(material -> material.getOrganizationId().equals(organizationId)
                        && material.getNumber().equals(number)
                        && material.getDeletedAt() == null);
    }
}
