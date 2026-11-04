package com.hydroyura.forgeflow.technology.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
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

class TechnologyProcessTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-01-02T00:00:00Z");

    private static TechnologyProcess process() {
        return TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", "Изготовление",
                PartId.generate(), OrganizationId.generate(), CREATED, UPDATED);
    }

    @Test
    void structure_hasEntityIdentityAndRequiredFields() {
        assertThat(TechnologyProcess.class.getAnnotation(Entity.class)).isNotNull();

        Map<String, Class<?>> fields = Arrays.stream(TechnologyProcess.class.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, Field::getType));

        assertThat(fields.keySet()).containsExactlyInAnyOrder(
                "id", "number", "name", "partId", "organizationId",
                "defaultProcess", "createdAt", "updatedAt", "deletedAt", "operations");
        assertThat(fields.get("id")).isEqualTo(TechnologyProcessId.class);
        assertThat(fields.get("number")).isEqualTo(String.class);
        assertThat(fields.get("name")).isEqualTo(String.class);
        assertThat(fields.get("partId")).isEqualTo(PartId.class);
        assertThat(fields.get("organizationId")).isEqualTo(OrganizationId.class);
        assertThat(fields.get("defaultProcess")).isEqualTo(boolean.class);
        assertThat(fields.get("createdAt")).isEqualTo(Instant.class);
        assertThat(fields.get("updatedAt")).isEqualTo(Instant.class);
        assertThat(fields.get("deletedAt")).isEqualTo(Instant.class);
        assertThat(fields.get("operations")).isEqualTo(List.class);

        Field idField = field("id");
        assertThat(idField.getAnnotation(Identity.class)).isNotNull();
    }

    @Test
    void create_withValidArguments_savesValuesAndDefaults() {
        TechnologyProcessId id = TechnologyProcessId.generate();
        PartId partId = PartId.generate();
        OrganizationId organizationId = OrganizationId.generate();

        TechnologyProcess process = TechnologyProcess.create(
                id, "TP-001", "Изготовление", partId, organizationId, CREATED, UPDATED);

        assertThat(process.getId()).isEqualTo(id);
        assertThat(process.getNumber()).isEqualTo("TP-001");
        assertThat(process.getName()).isEqualTo("Изготовление");
        assertThat(process.getPartId()).isEqualTo(partId);
        assertThat(process.getOrganizationId()).isEqualTo(organizationId);
        assertThat(process.getCreatedAt()).isEqualTo(CREATED);
        assertThat(process.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(process.isDefaultProcess()).isFalse();
        assertThat(process.getDeletedAt()).isNull();
        assertThat(process.operations()).isEmpty();
    }

    @Test
    void create_withBlankNumberOrName_rejectsAndIdentifiesField() {
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "   ", "Изготовление",
                PartId.generate(), OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), null, "Изготовление",
                PartId.generate(), OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", "",
                PartId.generate(), OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", null,
                PartId.generate(), OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_withNullRequiredReferences_rejects() {
        assertThatThrownBy(() -> TechnologyProcess.create(
                null, "TP-001", "Изготовление",
                PartId.generate(), OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", "Изготовление",
                null, OrganizationId.generate(), CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", "Изготовление",
                PartId.generate(), null, CREATED, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", "Изготовление",
                PartId.generate(), OrganizationId.generate(), null, UPDATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TechnologyProcess.create(
                TechnologyProcessId.generate(), "TP-001", "Изготовление",
                PartId.generate(), OrganizationId.generate(), CREATED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void auditFields_haveNoUserIdentifiers() {
        List<String> fieldNames = Arrays.stream(TechnologyProcess.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain("createdBy", "updatedBy");
    }

    @Test
    void rename_withValidName_updatesNameAndUpdatedAt() {
        TechnologyProcess process = process();

        process.rename("Новое имя");

        assertThat(process.getName()).isEqualTo("Новое имя");
        assertThat(process.getUpdatedAt()).isAfterOrEqualTo(UPDATED);
    }

    @Test
    void rename_withBlankOrNullName_rejectsAndKeepsName() {
        TechnologyProcess process = process();

        assertThatThrownBy(() -> process.rename("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> process.rename(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThat(process.getName()).isEqualTo("Изготовление");
    }

    @Test
    void markDeleted_onActiveProcess_setsDeletedAtAndUpdatesUpdatedAt() {
        TechnologyProcess process = process();

        process.markDeleted();

        assertThat(process.getDeletedAt()).isNotNull();
        assertThat(process.getUpdatedAt()).isEqualTo(process.getDeletedAt());
    }

    @Test
    void markDeleted_repeatedly_isIdempotent() {
        TechnologyProcess process = process();
        process.markDeleted();
        Instant deletedAt = process.getDeletedAt();
        Instant updatedAt = process.getUpdatedAt();

        process.markDeleted();

        assertThat(process.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(process.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void partId_isFinalAndHasNoMutator() {
        assertThat(Modifier.isFinal(field("partId").getModifiers())).isTrue();

        List<String> methodNames = Arrays.stream(TechnologyProcess.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();
        assertThat(methodNames).doesNotContain("setPartId", "changePartId");
    }

    @Test
    void structure_hasNoDuplicatedPartFields() {
        List<String> fieldNames = Arrays.stream(TechnologyProcess.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain("partNumber", "partName");
    }

    @Test
    void operations_addedOutOfOrder_areSortedByNumber() {
        TechnologyProcess process = process();

        process.addOperation(20, "А");
        process.addOperation(10, "Б");

        assertThat(process.operations())
                .extracting(Operation::number)
                .containsExactly(10, 20);
    }

    @Test
    void operations_returnedList_isUnmodifiable() {
        TechnologyProcess process = process();
        process.addOperation(10, "А");

        assertThatThrownBy(() -> process.operations().add(new Operation(20, "Б")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addOperation_withDuplicateNumber_rejects() {
        TechnologyProcess process = process();
        process.addOperation(10, "А");

        assertThatThrownBy(() -> process.addOperation(10, "Б"))
                .isInstanceOf(DuplicateOperationException.class);
    }

    @Test
    void addOperation_withNonPositiveNumber_rejects() {
        TechnologyProcess process = process();

        assertThatThrownBy(() -> process.addOperation(0, "А"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> process.addOperation(-1, "А"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addOperation_withBlankOrNullName_rejects() {
        TechnologyProcess process = process();

        assertThatThrownBy(() -> process.addOperation(1, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> process.addOperation(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeOperation_existing_removesIt() {
        TechnologyProcess process = process();
        process.addOperation(10, "А");

        process.removeOperation(10);

        assertThat(process.operations()).isEmpty();
    }

    @Test
    void removeOperation_missing_rejects() {
        TechnologyProcess process = process();

        assertThatThrownBy(() -> process.removeOperation(99))
                .isInstanceOf(OperationNotFoundException.class);
    }

    @Test
    void operations_withNumberGaps_keepAscendingOrder() {
        TechnologyProcess process = process();

        process.addOperation(30, "В");
        process.addOperation(10, "А");

        assertThat(process.operations())
                .extracting(Operation::number)
                .containsExactly(10, 30);
    }

    @Test
    void addAndRemoveOperation_updateUpdatedAt() {
        TechnologyProcess process = process();

        process.addOperation(10, "А");
        assertThat(process.getUpdatedAt()).isAfter(UPDATED);

        Instant afterAdd = process.getUpdatedAt();
        process.removeOperation(10);
        assertThat(process.getUpdatedAt()).isAfter(afterAdd);
    }

    @Test
    void markAsDefault_whenNotDefault_setsFlagAndUpdatesUpdatedAt() {
        TechnologyProcess process = process();

        process.markAsDefault();

        assertThat(process.isDefaultProcess()).isTrue();
        assertThat(process.getUpdatedAt()).isAfter(UPDATED);
    }

    @Test
    void markAsDefault_whenAlreadyDefault_isNoOp() {
        TechnologyProcess process = process();
        process.markAsDefault();
        Instant updatedAt = process.getUpdatedAt();

        process.markAsDefault();

        assertThat(process.isDefaultProcess()).isTrue();
        assertThat(process.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void markAsNotDefault_whenDefault_clearsFlagAndUpdatesUpdatedAt() {
        TechnologyProcess process = process();
        process.markAsDefault();
        Instant updatedAt = process.getUpdatedAt();

        process.markAsNotDefault();

        assertThat(process.isDefaultProcess()).isFalse();
        assertThat(process.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    void markAsNotDefault_whenAlreadyNotDefault_isNoOp() {
        TechnologyProcess process = process();
        Instant updatedAt = process.getUpdatedAt();

        process.markAsNotDefault();

        assertThat(process.isDefaultProcess()).isFalse();
        assertThat(process.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private static Field field(String name) {
        return Arrays.stream(TechnologyProcess.class.getDeclaredFields())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
