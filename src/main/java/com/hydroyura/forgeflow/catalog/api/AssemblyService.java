package com.hydroyura.forgeflow.catalog.api;

import java.util.List;

/**
 * Публичный контракт операций над сборочными единицами каталога.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно, а не передаётся клиентом.</p>
 */
public interface AssemblyService {

    AssemblyView create(CreateAssemblyRequest request);

    AssemblyView get(AssemblyId id);

    List<AssemblyView> list(AssemblyFilter filter);

    void rename(AssemblyId id, String newName);

    void delete(AssemblyId id);
}
