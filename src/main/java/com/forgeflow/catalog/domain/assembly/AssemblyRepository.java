package com.forgeflow.catalog.domain.assembly;

import com.forgeflow.catalog.api.AssemblyId;

/**
 * Driven port: сохранение и логическое удаление сборочных единиц.
 */
public interface AssemblyRepository {

    void save(Assembly assembly);

    void delete(AssemblyId id);
}
