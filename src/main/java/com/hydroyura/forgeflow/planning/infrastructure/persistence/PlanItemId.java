package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PlanItemId implements Serializable {

    public UUID planId;
    public UUID assemblyId;

    public PlanItemId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlanItemId other)) {
            return false;
        }
        return Objects.equals(planId, other.planId) && Objects.equals(assemblyId, other.assemblyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planId, assemblyId);
    }
}
