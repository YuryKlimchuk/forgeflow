package com.hydroyura.forgeflow.catalog.api;

import java.util.Objects;
import java.util.UUID;

public record BuyId(UUID value) {

    public BuyId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static BuyId generate() {
        return new BuyId(UUID.randomUUID());
    }

    public static BuyId of(UUID value) {
        return new BuyId(value);
    }
}
