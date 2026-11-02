package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class SubstitutionId implements Serializable {

    public UUID organizationId;
    public UUID assemblyId;
    public String itemType;
    public UUID itemId;
    public String substituteType;
    public UUID substituteId;

    public SubstitutionId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubstitutionId other)) {
            return false;
        }
        return Objects.equals(organizationId, other.organizationId)
                && Objects.equals(assemblyId, other.assemblyId)
                && Objects.equals(itemType, other.itemType)
                && Objects.equals(itemId, other.itemId)
                && Objects.equals(substituteType, other.substituteType)
                && Objects.equals(substituteId, other.substituteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId, assemblyId, itemType, itemId, substituteType, substituteId);
    }
}
