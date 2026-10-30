package com.forgeflow.catalog.domain.vzk;

import com.forgeflow.catalog.api.VzkId;

/**
 * Driven port: сохранение и логическое удаление групп ВЗК.
 */
public interface VzkRepository {

    void save(Vzk vzk);

    void delete(VzkId id);
}
