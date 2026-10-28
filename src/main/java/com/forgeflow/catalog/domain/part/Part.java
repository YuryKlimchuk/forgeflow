package com.forgeflow.catalog.domain.part;

import com.forgeflow.catalog.api.PartId;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public class Part {

    @Identity
    private final PartId id;

    private Part(PartId id) {
        this.id = id;
    }

    public static Part create(PartId id) {
        return new Part(Objects.requireNonNull(id, "id must not be null"));
    }

    public PartId getId() {
        return id;
    }
}
