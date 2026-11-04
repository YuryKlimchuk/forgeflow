package com.hydroyura.forgeflow.technology.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationTest {

    @Test
    void constructor_withValidValues_preservesThem() {
        Operation operation = new Operation(5, "Сверление");

        assertThat(operation.number()).isEqualTo(5);
        assertThat(operation.name()).isEqualTo("Сверление");
    }

    @Test
    void constructor_withMinimalNumber_accepts() {
        assertThat(new Operation(1, "Сверление").number()).isEqualTo(1);
    }

    @Test
    void constructor_withNonPositiveNumber_rejects() {
        assertThatThrownBy(() -> new Operation(0, "Сверление"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(-1, "Сверление"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_withMaxNumber_accepts() {
        assertThat(new Operation(Integer.MAX_VALUE, "Сверление").number()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void constructor_withBlankOrNullName_rejects() {
        assertThatThrownBy(() -> new Operation(1, ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
