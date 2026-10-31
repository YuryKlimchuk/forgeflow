package com.hydroyura.forgeflow.catalog.domain.assembly;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Driven port: сохранение и логическое удаление сборочных единиц.
 */
public interface AssemblyRepository {

    void save(Assembly assembly);

    void delete(AssemblyId id);
}
