package com.hydroyura.forgeflow.directories.domain.sortament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.directories.api.SortamentId;
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

class SortamentTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-01-02T00:00:00Z");

    private static Sortament sortament() {
        return Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30",
                StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, UPDATED);
    }

    @Test
    void structure_hasEntityIdentityAndRequiredFields() {
        assertThat(Sortament.class.getAnnotation(Entity.class)).isNotNull();

        Map<String, Class<?>> fields = Arrays.stream(Sortament.class.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Field::getType));

        assertThat(fields.keySet()).containsExactlyInAnyOrder(
                "id", "number", "name", "standardType", "standard", "unit",
                "organizationId", "createdAt", "updatedAt", "deletedAt");
        assertThat(fields.get("id")).isEqualTo(SortamentId.class);
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
        SortamentId id = SortamentId.generate();
        OrganizationId organizationId = OrganizationId.generate();

        Sortament sortament = Sortament.create(
                id, "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                organizationId, CREATED, UPDATED);

        assertThat(sortament.getId()).isEqualTo(id);
        assertThat(sortament.getNumber()).isEqualTo("Круг В-30");
        assertThat(sortament.getName()).isEqualTo("Круг горячекатаный В-30");
        assertThat(sortament.getStandardType()).isEqualTo(StandardType.GOST);
        assertThat(sortament.getStandard()).isEqualTo("ГОСТ 2590-2006");
        assertThat(sortament.getUnit()).isEqualTo("мм");
        assertThat(sortament.getOrganizationId()).isEqualTo(organizationId);
        assertThat(sortament.getCreatedAt()).isEqualTo(CREATED);
        assertThat(sortament.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(sortament.getDeletedAt()).isNull();
    }

    @Test
    void create_withBlankFields_rejectsAndIdentifiesField() {
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "   ", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "   ", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "   ", "мм",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standard");
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "   ",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");
    }

    @Test
    void create_withNullStandardType_rejects() {
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", null, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standardType");
    }

    @Test
    void create_withNullRequiredReferences_rejects() {
        assertThatThrownBy(() -> Sortament.create(
                null, "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), null, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void auditFields_haveNoUserIdentifiers() {
        List<String> fieldNames = Arrays.stream(Sortament.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain("createdBy", "updatedBy");
    }

    @Test
    void update_changesMutableFieldsAndUpdatedAt_keepsNumber() {
        Sortament sortament = sortament();

        sortament.update("Новое имя", StandardType.DIN, "DIN EN 10210", "см");

        assertThat(sortament.getName()).isEqualTo("Новое имя");
        assertThat(sortament.getStandardType()).isEqualTo(StandardType.DIN);
        assertThat(sortament.getStandard()).isEqualTo("DIN EN 10210");
        assertThat(sortament.getUnit()).isEqualTo("см");
        assertThat(sortament.getNumber()).isEqualTo("Круг В-30");
        assertThat(sortament.getUpdatedAt()).isAfterOrEqualTo(UPDATED);
    }

    @Test
    void update_withBlankOrNullValues_rejectsAndKeepsState() {
        Sortament sortament = sortament();

        assertThatThrownBy(() -> sortament.update("   ", StandardType.GOST, "ГОСТ 2590-2006", "мм"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> sortament.update("Круг горячекатаный В-30", null, "ГОСТ 2590-2006", "мм"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standardType");
        assertThatThrownBy(() -> sortament.update("Круг горячекатаный В-30", StandardType.GOST, "   ", "мм"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standard");
        assertThatThrownBy(() -> sortament.update("Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");

        assertThat(sortament.getName()).isEqualTo("Круг горячекатаный В-30");
        assertThat(sortament.getStandardType()).isEqualTo(StandardType.GOST);
        assertThat(sortament.getStandard()).isEqualTo("ГОСТ 2590-2006");
        assertThat(sortament.getUnit()).isEqualTo("мм");
    }

    @Test
    void markDeleted_onActiveSortament_setsDeletedAtAndUpdatedAt() {
        Sortament sortament = sortament();

        sortament.markDeleted();

        assertThat(sortament.getDeletedAt()).isNotNull();
        assertThat(sortament.getUpdatedAt()).isEqualTo(sortament.getDeletedAt());
    }

    @Test
    void markDeleted_repeatedly_isIdempotent() {
        Sortament sortament = sortament();
        sortament.markDeleted();
        Instant deletedAt = sortament.getDeletedAt();
        Instant updatedAt = sortament.getUpdatedAt();

        sortament.markDeleted();

        assertThat(sortament.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(sortament.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void number_isFinalAndHasNoMutator() {
        assertThat(Modifier.isFinal(field("number").getModifiers())).isTrue();

        List<String> methodNames = Arrays.stream(Sortament.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();
        assertThat(methodNames).doesNotContain("setNumber", "changeNumber");
        assertThat(methodNames).contains("update");
        assertThat(Arrays.stream(Sortament.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("update"))
                .findFirst()
                .orElseThrow()
                .getParameterTypes())
                .containsExactly(String.class, StandardType.class, String.class, String.class);
    }

    private static Field field(String name) {
        return Arrays.stream(Sortament.class.getDeclaredFields())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
