package com.hydroyura.forgeflow.catalog.api;

import java.util.List;

/**
 * Публичный контракт операций над стандартными изделиями каталога.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно, а не передаётся клиентом.</p>
 */
public interface StandartService {

    StandartView create(CreateStandartRequest request);

    StandartView get(StandartId id);

    List<StandartView> list(StandartFilter filter);

    void rename(StandartId id, String newName);

    void delete(StandartId id);
}
