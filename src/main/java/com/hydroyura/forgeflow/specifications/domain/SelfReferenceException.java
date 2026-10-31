package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Спецификация не может содержать собственную сборку.
 */
public class SelfReferenceException extends SpecificationsException {

    public SelfReferenceException(AssemblyId assemblyId) {
        super("Specification cannot contain its own assembly: " + assemblyId.value());
    }
}
