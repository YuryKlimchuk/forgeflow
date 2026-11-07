package com.hydroyura.forgeflow.directories.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SortamentIdTest {

    @Test
    void generate_returnsNonNullValue() {
        assertThat(SortamentId.generate().value()).isNotNull();
    }

    @Test
    void generate_returnsDistinctValues() {
        assertThat(SortamentId.generate().value()).isNotEqualTo(SortamentId.generate().value());
    }

    @Test
    void of_preservesValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(SortamentId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void of_rejectsNull() {
        assertThatThrownBy(() -> SortamentId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
