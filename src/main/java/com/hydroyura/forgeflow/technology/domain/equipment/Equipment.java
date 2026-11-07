package com.hydroyura.forgeflow.technology.domain.equipment;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Единица оборудования технологического процесса.
 *
 * <p>Агрегат хранит неизменяемый номер (обозначение), изменяемые наименование
 * и тип, организацию, аудит и логическое удаление.</p>
 */
@Entity
public class Equipment {

    @Identity
    private final EquipmentId id;

    private final String number;
    private String name;
    private String type;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Equipment(EquipmentId id, String number, String name, String type,
            OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.number = requireNotBlank(number, "number");
        this.name = requireNotBlank(name, "name");
        this.type = requireNotBlank(type, "type");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Equipment create(EquipmentId id, String number, String name, String type,
            OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        return new Equipment(id, number, name, type, organizationId, createdAt, updatedAt);
    }

    /**
     * Обновляет изменяемые поля, оставляя {@code number} неизменным.
     */
    public void update(String name, String type) {
        String newName = requireNotBlank(name, "name");
        String newType = requireNotBlank(type, "type");
        this.name = newName;
        this.type = newType;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        if (deletedAt != null) {
            return;
        }
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public EquipmentId getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    private static String requireNotBlank(String value, String field) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
