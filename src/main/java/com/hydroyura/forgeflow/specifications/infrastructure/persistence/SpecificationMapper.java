package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.domain.Quantity;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import java.util.List;

public final class SpecificationMapper {

    public List<SpecificationRowEntity> toEntities(Specification specification) {
        return specification.rows().stream()
                .map(row -> {
                    SpecificationRowEntity entity = new SpecificationRowEntity();
                    entity.organizationId = specification.organizationId().value();
                    entity.assemblyId = specification.assemblyId().value();
                    entity.itemType = row.item().type().name();
                    entity.itemId = row.item().id();
                    entity.quantity = row.quantity().value();
                    return entity;
                })
                .toList();
    }

    public Specification toDomain(
            OrganizationId organizationId, AssemblyId assemblyId, List<SpecificationRowEntity> rows) {
        Specification specification = Specification.create(organizationId, assemblyId);
        for (SpecificationRowEntity row : rows) {
            specification.addRow(
                    new CatalogItemReference(CatalogItemType.valueOf(row.itemType), row.itemId),
                    new Quantity(row.quantity));
        }
        return specification;
    }

    public static final SpecificationMapper INSTANCE = new SpecificationMapper();

    private SpecificationMapper() {
    }
}
