package com.hydroyura.forgeflow.directories.domain.material;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Материал справочника материалов.
 *
 * <p>Агрегат хранит неизменяемый номер (обозначение/марку), изменяемые
 * наименование, тип стандарта, обозначение стандарта и единицу измерения,
 * организацию, аудит и логическое удаление.</p>
 */
@Entity
public class Material {

    @Identity
    private final MaterialId id;

    private final String number;
    private String name;
    private StandardType standardType;
    private String standard;
    private String unit;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Material(MaterialId id, String number, String name, StandardType standardType,
            String standard, String unit, OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.number = requireNotBlank(number, "number");
        this.name = requireNotBlank(name, "name");
        this.standardType = requireStandardType(standardType);
        this.standard = requireNotBlank(standard, "standard");
        this.unit = requireNotBlank(unit, "unit");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Material create(MaterialId id, String number, String name, StandardType standardType,
            String standard, String unit, OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        return new Material(id, number, name, standardType, standard, unit, organizationId, createdAt, updatedAt);
    }

    /**
     * Обновляет изменяемые поля, оставляя {@code number} неизменным.
     */
    public void update(String name, StandardType standardType, String standard, String unit) {
        String newName = requireNotBlank(name, "name");
        StandardType newStandardType = requireStandardType(standardType);
        String newStandard = requireNotBlank(standard, "standard");
        String newUnit = requireNotBlank(unit, "unit");
        this.name = newName;
        this.standardType = newStandardType;
        this.standard = newStandard;
        this.unit = newUnit;
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

    public MaterialId getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public StandardType getStandardType() {
        return standardType;
    }

    public String getStandard() {
        return standard;
    }

    public String getUnit() {
        return unit;
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

    private static StandardType requireStandardType(StandardType value) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException("standardType must not be null");
        }
        return value;
    }
}
