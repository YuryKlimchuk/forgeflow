package com.hydroyura.forgeflow.catalog.infrastructure;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
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
    public void delete(AssemblyId id) {
        Assembly assembly = assemblies.get(id);
        if (assembly != null) {
            assembly.markDeleted();
        }
    }

    /** Диагностика для тестов и отладки (не расширяет доменный порт). */
    public Optional<Assembly> findById(AssemblyId id) {
        return Optional.ofNullable(assemblies.get(id));
    }

    /** Диагностика для тестов и отладки (не расширяет доменный порт). */
    public java.util.List<Assembly> findAll() {
        return java.util.List.copyOf(assemblies.values());
    }
}
