package com.hydroyura.forgeflow.catalog.api;

import java.util.List;

/**
 * Публичный контракт операций над группами ВЗК каталога.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно, а не передаётся клиентом.</p>
 */
public interface VzkService {

    VzkView create(CreateVzkRequest request);

    VzkView get(VzkId id);

    List<VzkView> list(VzkFilter filter);

    void rename(VzkId id, String newName);

    void delete(VzkId id);
}
