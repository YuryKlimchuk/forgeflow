package com.hydroyura.forgeflow.directories.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MaterialIdTest {

    @Test
    void generate_returnsNonNullValue() {
        assertThat(MaterialId.generate().value()).isNotNull();
    }

    @Test
    void generate_returnsDistinctValues() {
        assertThat(MaterialId.generate().value()).isNotEqualTo(MaterialId.generate().value());
    }

    @Test
    void of_preservesValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(MaterialId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void of_rejectsNull() {
        assertThatThrownBy(() -> MaterialId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
