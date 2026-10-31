package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Дополнительные (допустимые) замены одной строки спецификации.
 *
 * <p>Агрегат адресуется парой организация + сборка + основная ссылка строки.
 * Заменители однотипны с основной позицией, хранятся одноуровнево и не имеют
 * собственного количества — применяется количество основной строки.</p>
 */
public class Substitution {

    private final OrganizationId organizationId;
    private final AssemblyId assemblyId;
    private final CatalogItemReference item;
    private final List<CatalogItemReference> substitutes = new ArrayList<>();

    private Substitution(OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.assemblyId = Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        this.item = Objects.requireNonNull(item, "item must not be null");
    }

    public static Substitution create(
            OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        return new Substitution(organizationId, assemblyId, item);
    }

    public void addSubstitute(CatalogItemReference substitute) {
        Objects.requireNonNull(substitute, "substitute must not be null");
        if (item.type() != CatalogItemType.PART && item.type() != CatalogItemType.ASSEMBLY) {
            throw new InvalidSubstituteException(item, substitute);
        }
        if (substitute.type() != item.type()) {
            throw new InvalidSubstituteException(item, substitute);
        }
        if (item.equals(substitute)) {
            throw new InvalidSubstituteException(item, substitute);
        }
        if (substitutes.contains(substitute)) {
            throw new DuplicateSubstituteException(substitute);
        }
        substitutes.add(substitute);
    }

    public void removeSubstitute(CatalogItemReference substitute) {
        Objects.requireNonNull(substitute, "substitute must not be null");
        if (!substitutes.remove(substitute)) {
            throw new SubstituteNotFoundException(substitute);
        }
    }

    public List<CatalogItemReference> substitutes() {
        return Collections.unmodifiableList(substitutes);
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public AssemblyId assemblyId() {
        return assemblyId;
    }

    public CatalogItemReference item() {
        return item;
    }
}
