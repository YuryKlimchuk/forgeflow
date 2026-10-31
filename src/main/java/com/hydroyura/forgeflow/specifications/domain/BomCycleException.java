package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * В спецификациях обнаружен цикл вложенных сборок.
 */
public class BomCycleException extends SpecificationsException {

    public BomCycleException(AssemblyId assemblyId) {
        super("BOM cycle detected at assembly: " + assemblyId.value());
    }
}
