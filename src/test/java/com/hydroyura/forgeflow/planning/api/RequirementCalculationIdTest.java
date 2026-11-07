package com.hydroyura.forgeflow.planning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementCalculationIdTest {

    @Test
    void generateAndOf() {
        UUID value = UUID.randomUUID();

        assertThat(RequirementCalculationId.of(value).value()).isEqualTo(value);
        assertThat(RequirementCalculationId.generate().value()).isNotNull();
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> RequirementCalculationId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
