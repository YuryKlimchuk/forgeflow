package com.forgeflow.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogIdTest {

    @Test
    void assemblyId_generateReturnsUuid() {
        assertThat(AssemblyId.generate().value()).isNotNull();
    }

    @Test
    void assemblyId_ofPreservesValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(AssemblyId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void assemblyId_ofRejectsNull() {
        assertThatThrownBy(() -> AssemblyId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void partId_generateReturnsUuid() {
        assertThat(PartId.generate().value()).isNotNull();
    }

    @Test
    void partId_ofPreservesValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(PartId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void partId_ofRejectsNull() {
        assertThatThrownBy(() -> PartId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buyId_generateReturnsUuid() {
        assertThat(BuyId.generate().value()).isNotNull();
    }

    @Test
    void buyId_ofPreservesValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(BuyId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void buyId_ofRejectsNull() {
        assertThatThrownBy(() -> BuyId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void standartId_generateReturnsUuid() {
        assertThat(StandartId.generate().value()).isNotNull();
    }

    @Test
    void standartId_ofPreservesValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(StandartId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void standartId_ofRejectsNull() {
        assertThatThrownBy(() -> StandartId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void vzkId_generateReturnsUuid() {
        assertThat(VzkId.generate().value()).isNotNull();
    }

    @Test
    void vzkId_ofPreservesValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(VzkId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void vzkId_ofRejectsNull() {
        assertThatThrownBy(() -> VzkId.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
