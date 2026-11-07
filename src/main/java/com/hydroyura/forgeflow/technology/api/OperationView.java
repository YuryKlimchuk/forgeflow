package com.hydroyura.forgeflow.technology.api;

/**
 * Представление операции технологического процесса.
 *
 * <p>{@code equipmentId} опционален и равен {@code null} для операции без
 * оборудования.</p>
 */
public record OperationView(int number, String name, EquipmentId equipmentId) {

    /**
     * Обратно совместимый конструктор для операции без оборудования.
     */
    public OperationView(int number, String name) {
        this(number, name, null);
    }
}
