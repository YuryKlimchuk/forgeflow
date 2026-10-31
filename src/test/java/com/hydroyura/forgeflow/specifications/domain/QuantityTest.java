package com.hydroyura.forgeflow.specifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QuantityTest {

    @Test
    void acceptsOne() {
        assertThat(new Quantity(1).value()).isEqualTo(1);
    }

    @Test
    void acceptsMaxInt() {
        assertThat(new Quantity(Integer.MAX_VALUE).value()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void rejectsZero() {
        assertThatThrownBy(() -> new Quantity(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
