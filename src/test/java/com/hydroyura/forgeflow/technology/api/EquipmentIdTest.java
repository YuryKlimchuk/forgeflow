package com.hydroyura.forgeflow.technology.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EquipmentIdTest {

    @Test
    void generate_returnsDistinctNonNullUuids() {
        assertThat(EquipmentId.generate().value()).isNotNull();
        assertThat(EquipmentId.generate().value())
                .isNotEqualTo(EquipmentId.generate().value());
    }

    @Test
    void of_preservesValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(EquipmentId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void of_rejectsNull() {
        assertThatThrownBy(() -> EquipmentId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
