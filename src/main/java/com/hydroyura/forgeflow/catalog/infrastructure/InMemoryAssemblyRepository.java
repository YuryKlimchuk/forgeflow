package com.hydroyura.forgeflow.catalog.infrastructure;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link AssemblyRepository}.
 */
public class InMemoryAssemblyRepository implements AssemblyRepository {

    private final Map<AssemblyId, Assembly> assemblies = new ConcurrentHashMap<>();

    @Override
    public void save(Assembly assembly) {
        assemblies.put(assembly.getId(), assembly);
    }

    @Override
    public Optional<Assembly> findById(AssemblyId id, OrganizationId organizationId) {
        Assembly assembly = assemblies.get(id);
        if (assembly == null || !assembly.getOrganizationId().equals(organizationId) || assembly.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(assembly);
    }

    @Override
    public List<Assembly> findAll(OrganizationId organizationId) {
        return assemblies.values().stream()
                .filter(assembly -> assembly.getOrganizationId().equals(organizationId))
                .filter(assembly -> assembly.getDeletedAt() == null)
                .sorted(Comparator.comparing(Assembly::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return assemblies.values().stream()
                .anyMatch(assembly -> assembly.getOrganizationId().equals(organizationId)
                        && assembly.getNumber().equals(number)
                        && assembly.getDeletedAt() == null);
    }
}
