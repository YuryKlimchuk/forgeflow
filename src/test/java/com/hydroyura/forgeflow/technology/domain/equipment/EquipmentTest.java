package com.hydroyura.forgeflow.technology.domain.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.junit.jupiter.api.Test;

class EquipmentTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-01-02T00:00:00Z");

    private static Equipment equipment() {
        return Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "Токарный",
                OrganizationId.generate(), CREATED, UPDATED);
    }

    @Test
    void structure_hasEntityIdentityAndRequiredFields() {
        assertThat(Equipment.class.getAnnotation(Entity.class)).isNotNull();

        Map<String, Class<?>> fields = Arrays.stream(Equipment.class.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Field::getType));

        assertThat(fields.keySet()).containsExactlyInAnyOrder(
                "id", "number", "name", "type", "organizationId", "createdAt", "updatedAt", "deletedAt");
        assertThat(fields.get("id")).isEqualTo(EquipmentId.class);
        assertThat(fields.get("number")).isEqualTo(String.class);
        assertThat(fields.get("name")).isEqualTo(String.class);
        assertThat(fields.get("type")).isEqualTo(String.class);
        assertThat(fields.get("organizationId")).isEqualTo(OrganizationId.class);
        assertThat(fields.get("createdAt")).isEqualTo(Instant.class);
        assertThat(fields.get("updatedAt")).isEqualTo(Instant.class);
        assertThat(fields.get("deletedAt")).isEqualTo(Instant.class);

        assertThat(field("id").getAnnotation(Identity.class)).isNotNull();
    }

    @Test
    void create_withValidArguments_savesValues() {
        EquipmentId id = EquipmentId.generate();
        OrganizationId organizationId = OrganizationId.generate();

        Equipment equipment = Equipment.create(
                id, "С-001", "Станок токарный", "Токарный", organizationId, CREATED, UPDATED);

        assertThat(equipment.getId()).isEqualTo(id);
        assertThat(equipment.getNumber()).isEqualTo("С-001");
        assertThat(equipment.getName()).isEqualTo("Станок токарный");
        assertThat(equipment.getType()).isEqualTo("Токарный");
        assertThat(equipment.getOrganizationId()).isEqualTo(organizationId);
        assertThat(equipment.getCreatedAt()).isEqualTo(CREATED);
        assertThat(equipment.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(equipment.getDeletedAt()).isNull();
    }

    @Test
    void create_withBlankFields_rejectsAndIdentifiesField() {
        assertThatThrownBy(() -> Equipment.create(
                EquipmentId.generate(), "   ", "Станок токарный", "Токарный",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Equipment.create(
                EquipmentId.generate(), "С-001", "   ", "Токарный",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "   ",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void create_withNullRequiredReferences_rejects() {
        assertThatThrownBy(() -> Equipment.create(
                null, "С-001", "Станок токарный", "Токарный",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "Токарный",
                null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "Токарный",
                OrganizationId.generate(), null, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "Токарный",
                OrganizationId.generate(), CREATED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void auditFields_haveNoUserIdentifiers() {
        List<String> fieldNames = Arrays.stream(Equipment.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain("createdBy", "updatedBy");
    }

    @Test
    void update_changesMutableFieldsAndUpdatedAt_keepsNumber() {
        Equipment equipment = equipment();

        equipment.update("Новый станок", "Фрезерный");

        assertThat(equipment.getName()).isEqualTo("Новый станок");
        assertThat(equipment.getType()).isEqualTo("Фрезерный");
        assertThat(equipment.getNumber()).isEqualTo("С-001");
        assertThat(equipment.getUpdatedAt()).isAfterOrEqualTo(UPDATED);
    }

    @Test
    void update_withBlankValues_rejectsAndKeepsState() {
        Equipment equipment = equipment();

        assertThatThrownBy(() -> equipment.update("   ", "Токарный"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> equipment.update("Станок токарный", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");

        assertThat(equipment.getName()).isEqualTo("Станок токарный");
        assertThat(equipment.getType()).isEqualTo("Токарный");
    }

    @Test
    void markDeleted_onActiveEquipment_setsDeletedAtAndUpdatedAt() {
        Equipment equipment = equipment();

        equipment.markDeleted();

        assertThat(equipment.getDeletedAt()).isNotNull();
        assertThat(equipment.getUpdatedAt()).isEqualTo(equipment.getDeletedAt());
    }

    @Test
    void markDeleted_repeatedly_isIdempotent() {
        Equipment equipment = equipment();
        equipment.markDeleted();
        Instant deletedAt = equipment.getDeletedAt();
        Instant updatedAt = equipment.getUpdatedAt();

        equipment.markDeleted();

        assertThat(equipment.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(equipment.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void number_isFinalAndHasNoMutator() {
        assertThat(Modifier.isFinal(field("number").getModifiers())).isTrue();

        List<String> methodNames = Arrays.stream(Equipment.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();
        assertThat(methodNames).doesNotContain("setNumber", "changeNumber");
        assertThat(methodNames).contains("update");
        assertThat(Arrays.stream(Equipment.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("update"))
                .findFirst()
                .orElseThrow()
                .getParameterTypes())
                .containsExactly(String.class, String.class);
    }

    private static Field field(String name) {
        return Arrays.stream(Equipment.class.getDeclaredFields())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
