package com.hydroyura.forgeflow.directories.api;

import java.util.List;

/**
 * Публичный контракт управления материалами.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface MaterialService {

    /**
     * Создаёт активный материал с уникальным в пределах организации номером.
     */
    MaterialView create(CreateMaterialRequest request);

    /**
     * Возвращает активный материал текущей организации.
     *
     * @throws com.hydroyura.forgeflow.directories.domain.material.MaterialNotFoundException
     *         если материал отсутствует, удалён или принадлежит другой организации
     */
    MaterialView get(MaterialId id);

    /**
     * Возвращает активные материалы текущей организации, отсортированные по номеру.
     */
    List<MaterialView> list();

    /**
     * Обновляет изменяемые поля существующего активного материала.
     */
    void update(MaterialId id, UpdateMaterialRequest request);

    /**
     * Логически удаляет (soft-delete) активный материал текущей организации.
     */
    void delete(MaterialId id);
}
