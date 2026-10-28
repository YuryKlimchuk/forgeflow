package com.forgeflow.catalog.domain.buy;

import com.forgeflow.catalog.api.BuyId;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public class Buy {

    @Identity
    private final BuyId id;

    private Buy(BuyId id) {
        this.id = id;
    }

    public static Buy create(BuyId id) {
        return new Buy(Objects.requireNonNull(id, "id must not be null"));
    }

    public BuyId getId() {
        return id;
    }
}
