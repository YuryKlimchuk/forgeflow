package com.hydroyura.forgeflow.planning.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NeedTypeTest {

    @Test
    void containsOnlyManufactureAndPurchase() {
        assertThat(NeedType.values()).containsExactly(NeedType.MANUFACTURE, NeedType.PURCHASE);
    }
}
