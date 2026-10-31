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
}
