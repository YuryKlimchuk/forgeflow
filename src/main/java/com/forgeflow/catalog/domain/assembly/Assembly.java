package com.forgeflow.catalog.domain.assembly;

import com.forgeflow.catalog.api.AssemblyId;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public class Assembly {

    @Identity
    private final AssemblyId id;

    private Assembly(AssemblyId id) {
        this.id = id;
    }

    public static Assembly create(AssemblyId id) {
        return new Assembly(Objects.requireNonNull(id, "id must not be null"));
    }

    public AssemblyId getId() {
        return id;
    }
}
