package com.hydroyura.forgeflow.technology.domain;

import com.hydroyura.forgeflow.technology.api.EquipmentId;
import org.apache.commons.lang3.StringUtils;

/**
 * Операция технологического процесса: порядковый номер, наименование и
 * необязательная ссылка на оборудование.
 *
 * <p>Неизменяемое значение, идентифицируется внутри процесса по номеру.
 * {@code equipmentId = null} означает операцию без оборудования.</p>
 */
public record Operation(int number, String name, EquipmentId equipmentId) {

    /**
     * Обратно совместимый конструктор для операции без оборудования.
     */
    public Operation(int number, String name) {
        this(number, name, null);
    }

    public Operation {
        if (number <= 0) {
            throw new IllegalArgumentException("number must be positive");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
