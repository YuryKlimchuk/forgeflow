package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.domain.Substitution;
import java.util.List;

public final class SubstitutionMapper {

    public List<SubstitutionEntity> toEntities(Substitution substitution) {
        return substitution.substitutes().stream()
                .map(substitute -> {
                    SubstitutionEntity entity = new SubstitutionEntity();
                    entity.organizationId = substitution.organizationId().value();
                    entity.assemblyId = substitution.assemblyId().value();
                    entity.itemType = substitution.item().type().name();
                    entity.itemId = substitution.item().id();
                    entity.substituteType = substitute.type().name();
                    entity.substituteId = substitute.id();
                    return entity;
                })
                .toList();
    }

    public Substitution toDomain(
            OrganizationId organizationId,
            AssemblyId assemblyId,
            CatalogItemReference item,
            List<SubstitutionEntity> rows) {
        Substitution substitution = Substitution.create(organizationId, assemblyId, item);
        for (SubstitutionEntity row : rows) {
            substitution.addSubstitute(
                    new CatalogItemReference(CatalogItemType.valueOf(row.substituteType), row.substituteId));
        }
        return substitution;
    }

    public static final SubstitutionMapper INSTANCE = new SubstitutionMapper();

    private SubstitutionMapper() {
    }
}
