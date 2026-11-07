package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RequirementCalculationItemSourceId implements Serializable {

    public UUID calculationItemId;
    public UUID sourceAssemblyId;

    public RequirementCalculationItemSourceId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RequirementCalculationItemSourceId other)) {
            return false;
        }
        return Objects.equals(calculationItemId, other.calculationItemId)
                && Objects.equals(sourceAssemblyId, other.sourceAssemblyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(calculationItemId, sourceAssemblyId);
    }
}
