package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class SpecificationRowId implements Serializable {

    public UUID organizationId;
    public UUID assemblyId;
    public String itemType;
    public UUID itemId;

    public SpecificationRowId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpecificationRowId other)) {
            return false;
        }
        return Objects.equals(organizationId, other.organizationId)
                && Objects.equals(assemblyId, other.assemblyId)
                && Objects.equals(itemType, other.itemType)
                && Objects.equals(itemId, other.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId, assemblyId, itemType, itemId);
    }
}
