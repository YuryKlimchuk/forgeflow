package com.hydroyura.forgeflow.directories.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StandardTypeTest {

    @Test
    void values_containExactlyGostDinIsoOther() {
        assertThat(StandardType.values())
                .containsExactly(StandardType.GOST, StandardType.DIN, StandardType.ISO, StandardType.OTHER);
    }
}
