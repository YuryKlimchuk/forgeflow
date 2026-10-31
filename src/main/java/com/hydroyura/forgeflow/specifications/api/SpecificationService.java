package com.hydroyura.forgeflow.specifications.api;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import java.util.List;

/**
 * Публичный контракт управления строками спецификации сборочной единицы.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface SpecificationService {

    /**
     * Возвращает прямые строки спецификации сборки текущей организации.
     */
    List<SpecificationRowView> get(AssemblyId assemblyId);

    /**
     * Добавляет строку и возвращает её представление.
     */
    SpecificationRowView addRow(AssemblyId assemblyId, AddRowRequest request);

    /**
     * Изменяет количество существующей строки.
     */
    void changeQuantity(AssemblyId assemblyId, ChangeQuantityRequest request);

    /**
     * Удаляет существующую строку.
     */
    void removeRow(AssemblyId assemblyId, RemoveRowRequest request);

    /**
     * Копирует спецификацию в новую сборку с переданными номером и именем.
     */
    AssemblyView copy(AssemblyId sourceAssemblyId, CopySpecificationRequest request);

    /**
     * Возвращает плоский состав спецификации: только листовые элементы с
     * просуммированным количеством, отсортированные по номеру.
     */
    List<FlatBomItem> flatten(AssemblyId assemblyId);

    /**
     * Возвращает список доп. замен существующей строки текущей организации.
     */
    List<CatalogItemReference> listSubstitutes(AssemblyId assemblyId, CatalogItemReference item);

    /**
     * Добавляет доп. замену к существующей строке и возвращает её ссылку.
     */
    CatalogItemReference addSubstitute(AssemblyId assemblyId, AddSubstituteRequest request);

    /**
     * Удаляет доп. замену у существующей строки.
     */
    void removeSubstitute(AssemblyId assemblyId, RemoveSubstituteRequest request);
}
