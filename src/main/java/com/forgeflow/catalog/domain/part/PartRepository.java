package com.forgeflow.catalog.domain.part;

import com.forgeflow.catalog.api.PartId;

/**
 * Driven port: сохранение и логическое удаление деталей.
 */
public interface PartRepository {

    void save(Part part);

    void delete(PartId id);
}
