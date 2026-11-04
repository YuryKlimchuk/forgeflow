package com.hydroyura.forgeflow.technology.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TechnologyProcessIdTest {

    @Test
    void generate_returnsDistinctNonNullUuids() {
        assertThat(TechnologyProcessId.generate().value()).isNotNull();
        assertThat(TechnologyProcessId.generate().value())
                .isNotEqualTo(TechnologyProcessId.generate().value());
    }

    @Test
    void of_preservesValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(TechnologyProcessId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void of_rejectsNull() {
        assertThatThrownBy(() -> TechnologyProcessId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
