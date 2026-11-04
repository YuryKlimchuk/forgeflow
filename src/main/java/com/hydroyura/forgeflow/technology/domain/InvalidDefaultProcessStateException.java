package com.hydroyura.forgeflow.technology.domain;

import com.hydroyura.forgeflow.catalog.api.PartId;

/**
 * Нарушен инвариант дефолтного технологического процесса детали.
 */
public class InvalidDefaultProcessStateException extends RuntimeException {

    public InvalidDefaultProcessStateException(PartId partId) {
        super("Invalid default process state for part: " + partId);
    }
}
