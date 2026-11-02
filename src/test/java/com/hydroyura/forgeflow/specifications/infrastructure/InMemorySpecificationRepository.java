package com.hydroyura.forgeflow.specifications.infrastructure;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link SpecificationRepository}: ключ хранения включает
 * организацию и сборку, что обеспечивает tenant-изоляцию.
 */
public class InMemorySpecificationRepository implements SpecificationRepository {

    private final Map<OrganizationId, Map<AssemblyId, Specification>> byOrganization = new ConcurrentHashMap<>();

    @Override
    public void save(Specification specification) {
        byOrganization.computeIfAbsent(specification.organizationId(), ignored -> new ConcurrentHashMap<>())
                .put(specification.assemblyId(), specification);
    }

    @Override
    public Optional<Specification> findById(OrganizationId organizationId, AssemblyId assemblyId) {
        Map<AssemblyId, Specification> specifications = byOrganization.get(organizationId);
        return Optional.ofNullable(specifications == null ? null : specifications.get(assemblyId));
    }
}
