package com.forgeflow.catalog.domain.standart;

import com.forgeflow.catalog.api.StandartId;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public class Standart {

    @Identity
    private final StandartId id;

    private Standart(StandartId id) {
        this.id = id;
    }

    public static Standart create(StandartId id) {
        return new Standart(Objects.requireNonNull(id, "id must not be null"));
    }

    public StandartId getId() {
        return id;
    }
}
