package com.forgeflow.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.catalog.api.AssemblyId;
import com.forgeflow.catalog.api.BuyId;
import com.forgeflow.catalog.api.PartId;
import com.forgeflow.catalog.api.StandartId;
import com.forgeflow.catalog.api.VzkId;
import com.forgeflow.catalog.domain.assembly.Assembly;
import com.forgeflow.catalog.domain.buy.Buy;
import com.forgeflow.catalog.domain.part.Part;
import com.forgeflow.catalog.domain.standart.Standart;
import com.forgeflow.catalog.domain.vzk.Vzk;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.junit.jupiter.api.Test;

class CatalogEntityTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void assembly_structureStorageValidationAndSoftDelete() {
        assertEntityStructure(Assembly.class, AssemblyId.class);

        var id = AssemblyId.generate();
        var organizationId = UUID.randomUUID();
        var assembly = Assembly.create(id, "A-001", "Узел", organizationId, CREATED, UPDATED);

        assertThat(assembly.getId()).isEqualTo(id);
        assertThat(assembly.getNumber()).isEqualTo("A-001");
        assertThat(assembly.getName()).isEqualTo("Узел");
        assertThat(assembly.getOrganizationId()).isEqualTo(organizationId);
        assertThat(assembly.getCreatedAt()).isEqualTo(CREATED);
        assertThat(assembly.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(assembly.getDeletedAt()).isNull();

        assertThatThrownBy(() -> Assembly.create(id, "   ", "Узел", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Assembly.create(id, "A-001", "", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Assembly.create(null, "A-001", "Узел", organizationId, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assembly.create(id, "A-001", "Узел", null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assembly.create(id, "A-001", "Узел", organizationId, null, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Assembly.create(id, "A-001", "Узел", organizationId, CREATED, null))
                .isInstanceOf(NullPointerException.class);

        assertSoftDelete(assembly::markDeleted, assembly::getDeletedAt, assembly::getUpdatedAt);
    }

    @Test
    void part_structureStorageValidationAndSoftDelete() {
        assertEntityStructure(Part.class, PartId.class);

        var id = PartId.generate();
        var organizationId = UUID.randomUUID();
        var part = Part.create(id, "P-001", "Деталь", organizationId, CREATED, UPDATED);

        assertThat(part.getId()).isEqualTo(id);
        assertThat(part.getNumber()).isEqualTo("P-001");
        assertThat(part.getName()).isEqualTo("Деталь");
        assertThat(part.getOrganizationId()).isEqualTo(organizationId);
        assertThat(part.getCreatedAt()).isEqualTo(CREATED);
        assertThat(part.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(part.getDeletedAt()).isNull();

        assertThatThrownBy(() -> Part.create(id, "   ", "Деталь", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Part.create(id, "P-001", "", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Part.create(id, "P-001", "Деталь", null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);

        assertSoftDelete(part::markDeleted, part::getDeletedAt, part::getUpdatedAt);
    }

    @Test
    void buy_structureStorageValidationAndSoftDelete() {
        assertEntityStructure(Buy.class, BuyId.class);

        var id = BuyId.generate();
        var organizationId = UUID.randomUUID();
        var buy = Buy.create(id, "B-001", "Покупное изделие", organizationId, CREATED, UPDATED);

        assertThat(buy.getId()).isEqualTo(id);
        assertThat(buy.getNumber()).isEqualTo("B-001");
        assertThat(buy.getName()).isEqualTo("Покупное изделие");
        assertThat(buy.getOrganizationId()).isEqualTo(organizationId);
        assertThat(buy.getCreatedAt()).isEqualTo(CREATED);
        assertThat(buy.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(buy.getDeletedAt()).isNull();

        assertThatThrownBy(() -> Buy.create(id, "   ", "Покупное изделие", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Buy.create(id, "B-001", "", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Buy.create(id, "B-001", "Покупное изделие", null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);

        assertSoftDelete(buy::markDeleted, buy::getDeletedAt, buy::getUpdatedAt);
    }

    @Test
    void standart_structureStorageValidationAndSoftDelete() {
        assertEntityStructure(Standart.class, StandartId.class);

        var id = StandartId.generate();
        var organizationId = UUID.randomUUID();
        var standart = Standart.create(id, "S-001", "Стандартное изделие", organizationId, CREATED, UPDATED);

        assertThat(standart.getId()).isEqualTo(id);
        assertThat(standart.getNumber()).isEqualTo("S-001");
        assertThat(standart.getName()).isEqualTo("Стандартное изделие");
        assertThat(standart.getOrganizationId()).isEqualTo(organizationId);
        assertThat(standart.getCreatedAt()).isEqualTo(CREATED);
        assertThat(standart.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(standart.getDeletedAt()).isNull();

        assertThatThrownBy(() -> Standart.create(id, "   ", "Стандартное изделие", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Standart.create(id, "S-001", "", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Standart.create(id, "S-001", "Стандартное изделие", null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);

        assertSoftDelete(standart::markDeleted, standart::getDeletedAt, standart::getUpdatedAt);
    }

    @Test
    void vzk_structureStorageValidationAndSoftDelete() {
        assertEntityStructure(Vzk.class, VzkId.class);

        var id = VzkId.generate();
        var organizationId = UUID.randomUUID();
        var vzk = Vzk.create(id, "V-001", "Группа ВЗК", organizationId, CREATED, UPDATED);

        assertThat(vzk.getId()).isEqualTo(id);
        assertThat(vzk.getNumber()).isEqualTo("V-001");
        assertThat(vzk.getName()).isEqualTo("Группа ВЗК");
        assertThat(vzk.getOrganizationId()).isEqualTo(organizationId);
        assertThat(vzk.getCreatedAt()).isEqualTo(CREATED);
        assertThat(vzk.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(vzk.getDeletedAt()).isNull();

        assertThatThrownBy(() -> Vzk.create(id, "   ", "Группа ВЗК", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> Vzk.create(id, "V-001", "", organizationId, CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Vzk.create(id, "V-001", "Группа ВЗК", null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);

        assertSoftDelete(vzk::markDeleted, vzk::getDeletedAt, vzk::getUpdatedAt);
    }

    private static void assertEntityStructure(Class<?> entityClass, Class<?> idClass) {
        assertThat(entityClass.getAnnotation(Entity.class)).isNotNull();

        Map<String, Class<?>> fields = Arrays.stream(entityClass.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Field::getType));

        assertThat(fields.keySet()).containsExactlyInAnyOrder(
                "id", "number", "name", "organizationId", "createdAt", "updatedAt", "deletedAt");
        assertThat(fields.get("id")).isEqualTo(idClass);
        assertThat(fields.get("number")).isEqualTo(String.class);
        assertThat(fields.get("name")).isEqualTo(String.class);
        assertThat(fields.get("organizationId")).isEqualTo(UUID.class);
        assertThat(fields.get("createdAt")).isEqualTo(Instant.class);
        assertThat(fields.get("updatedAt")).isEqualTo(Instant.class);
        assertThat(fields.get("deletedAt")).isEqualTo(Instant.class);

        Field idField = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.getName().equals("id"))
                .findFirst()
                .orElseThrow();
        assertThat(idField.getAnnotation(Identity.class)).isNotNull();
    }

    private static void assertSoftDelete(Runnable markDeleted, java.util.function.Supplier<Instant> getDeletedAt,
            java.util.function.Supplier<Instant> getUpdatedAt) {
        markDeleted.run();
        Instant deletedAt = getDeletedAt.get();
        assertThat(deletedAt).isNotNull();
        assertThat(getUpdatedAt.get()).isEqualTo(deletedAt);

        markDeleted.run();
        assertThat(getDeletedAt.get()).isEqualTo(deletedAt);
    }
}
