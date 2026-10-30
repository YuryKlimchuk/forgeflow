package com.forgeflow.catalog.domain.standart;

import com.forgeflow.catalog.api.StandartId;

/**
 * Driven port: сохранение и логическое удаление стандартных изделий.
 */
public interface StandartRepository {

    void save(Standart standart);

    void delete(StandartId id);
}
