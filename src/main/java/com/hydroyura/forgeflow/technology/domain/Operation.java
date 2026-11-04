package com.hydroyura.forgeflow.technology.domain;

import org.apache.commons.lang3.StringUtils;

/**
 * Операция технологического процесса: порядковый номер и наименование.
 *
 * <p>Неизменяемое значение, идентифицируется внутри процесса по номеру.</p>
 */
public record Operation(int number, String name) {

    public Operation {
        if (number <= 0) {
            throw new IllegalArgumentException("number must be positive");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
