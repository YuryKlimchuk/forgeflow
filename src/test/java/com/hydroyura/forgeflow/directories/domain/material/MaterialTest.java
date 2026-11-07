package com.hydroyura.forgeflow.directories.domain.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
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

class MaterialTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-01-02T00:00:00Z");

    private static Material material() {
        return Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, UPDATED);
    }

    @Test
    void structure_hasEntityIdentityAndRequiredFields() {
        assertThat(Material.class.getAnnotation(Entity.class)).isNotNull();

        Map<String, Class<?>> fields = Arrays.stream(Material.class.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Field::getType));

        assertThat(fields.keySet()).containsExactlyInAnyOrder(
                "id", "number", "name", "standardType", "standard", "unit",
                "organizationId", "createdAt", "updatedAt", "deletedAt");
        assertThat(fields.get("id")).isEqualTo(MaterialId.class);
        assertThat(fields.get("number")).isEqualTo(String.class);
        assertThat(fields.get("name")).isEqualTo(String.class);
        assertThat(fields.get("standardType")).isEqualTo(StandardType.class);
        assertThat(fields.get("standard")).isEqualTo(String.class);
        assertThat(fields.get("unit")).isEqualTo(String.class);
        assertThat(fields.get("organizationId")).isEqualTo(OrganizationId.class);
        assertThat(fields.get("createdAt")).isEqualTo(Instant.class);
        assertThat(fields.get("updatedAt")).isEqualTo(Instant.class);
        assertThat(fields.get("deletedAt")).isEqualTo(Instant.class);

        assertThat(field("id").getAnnotation(Identity.class)).isNotNull();
    }

    @Test
    void create_withValidArguments_savesValues() {
        MaterialId id = MaterialId.generate();
        OrganizationId organizationId = OrganizationId.generate();

        Material material = Material.create(
                id, "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                organizationId, CREATED, UPDATED);

        assertThat(material.getId()).isEqualTo(id);
        assertThat(material.getNumber()).isEqualTo("45");
        assertThat(material.getName()).isEqualTo("Сталь 45");
        assertThat(material.getStandardType()).isEqualTo(StandardType.GOST);
        assertThat(material.getStandard()).isEqualTo("ГОСТ 1050-2013");
        assertThat(material.getUnit()).isEqualTo("кг");
        assertThat(material.getOrganizationId()).isEqualTo(organizationId);
        assertThat(material.getCreatedAt()).isEqualTo(CREATED);
        assertThat(material.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(material.getDeletedAt()).isNull();
    }

    @Test
    void create_withBlankFields_rejectsAndIdentifiesField() {
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "   ", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "   ", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "   ", "кг",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standard");
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "   ",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");
    }

    @Test
    void create_withNullStandardType_rejects() {
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "Сталь 45", null, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standardType");
    }

    @Test
    void create_withNullRequiredReferences_rejects() {
        assertThatThrownBy(() -> Material.create(
                null, "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), null, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void auditFields_haveNoUserIdentifiers() {
        List<String> fieldNames = Arrays.stream(Material.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain("createdBy", "updatedBy");
    }

    @Test
    void update_changesMutableFieldsAndUpdatedAt_keepsNumber() {
        Material material = material();

        material.update("Новое имя", StandardType.DIN, "DIN EN 10088-2", "м");

        assertThat(material.getName()).isEqualTo("Новое имя");
        assertThat(material.getStandardType()).isEqualTo(StandardType.DIN);
        assertThat(material.getStandard()).isEqualTo("DIN EN 10088-2");
        assertThat(material.getUnit()).isEqualTo("м");
        assertThat(material.getNumber()).isEqualTo("45");
        assertThat(material.getUpdatedAt()).isAfterOrEqualTo(UPDATED);
    }

    @Test
    void update_withBlankOrNullValues_rejectsAndKeepsState() {
        Material material = material();

        assertThatThrownBy(() -> material.update("   ", StandardType.GOST, "ГОСТ 1050-2013", "кг"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> material.update("Сталь 45", null, "ГОСТ 1050-2013", "кг"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standardType");
        assertThatThrownBy(() -> material.update("Сталь 45", StandardType.GOST, "   ", "кг"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standard");
        assertThatThrownBy(() -> material.update("Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");

        assertThat(material.getName()).isEqualTo("Сталь 45");
        assertThat(material.getStandardType()).isEqualTo(StandardType.GOST);
        assertThat(material.getStandard()).isEqualTo("ГОСТ 1050-2013");
        assertThat(material.getUnit()).isEqualTo("кг");
    }

    @Test
    void markDeleted_onActiveMaterial_setsDeletedAtAndUpdatedAt() {
        Material material = material();

        material.markDeleted();

        assertThat(material.getDeletedAt()).isNotNull();
        assertThat(material.getUpdatedAt()).isEqualTo(material.getDeletedAt());
    }

    @Test
    void markDeleted_repeatedly_isIdempotent() {
        Material material = material();
        material.markDeleted();
        Instant deletedAt = material.getDeletedAt();
        Instant updatedAt = material.getUpdatedAt();

        material.markDeleted();

        assertThat(material.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(material.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void number_isFinalAndHasNoMutator() {
        assertThat(Modifier.isFinal(field("number").getModifiers())).isTrue();

        List<String> methodNames = Arrays.stream(Material.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();
        assertThat(methodNames).doesNotContain("setNumber", "changeNumber");
        assertThat(methodNames).contains("update");
        assertThat(Arrays.stream(Material.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("update"))
                .findFirst()
                .orElseThrow()
                .getParameterTypes())
                .containsExactly(String.class, StandardType.class, String.class, String.class);
    }

    private static Field field(String name) {
        return Arrays.stream(Material.class.getDeclaredFields())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
