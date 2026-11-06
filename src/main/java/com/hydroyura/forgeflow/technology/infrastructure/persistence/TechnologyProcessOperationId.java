package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TechnologyProcessOperationId implements Serializable {

    public UUID processId;
    public int operationNumber;

    public TechnologyProcessOperationId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TechnologyProcessOperationId other)) {
            return false;
        }
        return operationNumber == other.operationNumber
                && Objects.equals(processId, other.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, operationNumber);
    }
}
