package com.hydroyura.forgeflow.technology.api;

import java.util.List;

/**
 * Публичный контракт управления оборудованием.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface EquipmentService {

    /**
     * Создаёт активное оборудование с уникальным в пределах организации номером.
     */
    EquipmentView create(CreateEquipmentRequest request);

    /**
     * Возвращает активное оборудование текущей организации.
     *
     * @throws com.hydroyura.forgeflow.technology.domain.equipment.EquipmentNotFoundException
     *         если оборудование отсутствует, удалено или принадлежит другой организации
     */
    EquipmentView get(EquipmentId id);

    /**
     * Возвращает активное оборудование текущей организации, отсортированное по номеру.
     */
    List<EquipmentView> list();

    /**
     * Обновляет изменяемые поля существующего активного оборудования.
     */
    void update(EquipmentId id, UpdateEquipmentRequest request);

    /**
     * Логически удаляет (soft-delete) активное оборудование текущей организации.
     */
    void delete(EquipmentId id);
}
