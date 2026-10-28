package com.forgeflow.catalog.domain.vzk;

import com.forgeflow.catalog.api.VzkId;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public class Vzk {

    @Identity
    private final VzkId id;

    private Vzk(VzkId id) {
        this.id = id;
    }

    public static Vzk create(VzkId id) {
        return new Vzk(Objects.requireNonNull(id, "id must not be null"));
    }

    public VzkId getId() {
        return id;
    }
}
