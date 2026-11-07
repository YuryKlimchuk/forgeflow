package com.hydroyura.forgeflow.technology.api;

/**
 * Входной контракт добавления операции в технологический процесс.
 *
 * <p>{@code equipmentId} опционален и может быть {@code null}, если операция
 * не привязана к оборудованию.</p>
 */
public record AddOperationRequest(int number, String name, EquipmentId equipmentId) {

    /**
     * Обратно совместимый конструктор для операции без оборудования.
     */
    public AddOperationRequest(int number, String name) {
        this(number, name, null);
    }
}
