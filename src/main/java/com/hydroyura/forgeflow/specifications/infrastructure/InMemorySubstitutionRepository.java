package com.hydroyura.forgeflow.specifications.infrastructure;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.domain.Substitution;
import com.hydroyura.forgeflow.specifications.domain.SubstitutionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link SubstitutionRepository}: ключ хранения включает
 * организацию, сборку и основную ссылку строки, что обеспечивает
 * tenant-изоляцию.
 */
public class InMemorySubstitutionRepository implements SubstitutionRepository {

    private final Map<OrganizationId, Map<AssemblyId, Map<CatalogItemReference, Substitution>>> byOrganization =
            new ConcurrentHashMap<>();

    @Override
    public void save(Substitution substitution) {
        byOrganization
                .computeIfAbsent(substitution.organizationId(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(substitution.assemblyId(), ignored -> new ConcurrentHashMap<>())
                .put(substitution.item(), substitution);
    }

    @Override
    public Optional<Substitution> findById(
            OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        Map<AssemblyId, Map<CatalogItemReference, Substitution>> byAssembly = byOrganization.get(organizationId);
        if (byAssembly == null) {
            return Optional.empty();
        }
        Map<CatalogItemReference, Substitution> byItem = byAssembly.get(assemblyId);
        return Optional.ofNullable(byItem == null ? null : byItem.get(item));
    }

    @Override
    public void delete(OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        Map<AssemblyId, Map<CatalogItemReference, Substitution>> byAssembly = byOrganization.get(organizationId);
        if (byAssembly == null) {
            return;
        }
        Map<CatalogItemReference, Substitution> byItem = byAssembly.get(assemblyId);
        if (byItem == null) {
            return;
        }
        byItem.remove(item);
    }
}
