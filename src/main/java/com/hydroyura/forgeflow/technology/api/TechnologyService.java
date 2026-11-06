package com.hydroyura.forgeflow.technology.api;

import com.hydroyura.forgeflow.catalog.api.PartId;
import java.util.List;

/**
 * Публичный контракт управления технологическими процессами.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface TechnologyService {

    TechnologyProcessView create(CreateTechnologyProcessRequest request);

    TechnologyProcessView get(TechnologyProcessId id);

    /**
     * Возвращает активные процессы текущей организации, отсортированные по номеру.
     *
     * @param partId идентификатор детали для фильтра; {@code null} — без фильтра
     */
    List<TechnologyProcessView> list(PartId partId);

    void rename(TechnologyProcessId id, String newName);

    void delete(TechnologyProcessId id);

    OperationView addOperation(TechnologyProcessId id, AddOperationRequest request);

    void removeOperation(TechnologyProcessId id, int number);

    void markAsDefault(TechnologyProcessId id);

    void unmarkDefault(TechnologyProcessId id);
}
