package com.hydroyura.forgeflow.technology.domain;

import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;

/**
 * Технологический процесс отсутствует или принадлежит другой организации.
 */
public class TechnologyProcessNotFoundException extends RuntimeException {

    public TechnologyProcessNotFoundException(TechnologyProcessId id) {
        super("Technology process not found: " + id);
    }
}
