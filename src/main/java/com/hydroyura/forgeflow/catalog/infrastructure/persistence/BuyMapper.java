package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

public final class BuyMapper {

    public BuyEntity toEntity(Buy buy) {
        BuyEntity entity = new BuyEntity();
        entity.id = buy.getId().value();
        entity.number = buy.getNumber();
        entity.name = buy.getName();
        entity.organizationId = buy.getOrganizationId().value();
        entity.createdAt = buy.getCreatedAt();
        entity.updatedAt = buy.getUpdatedAt();
        entity.deletedAt = buy.getDeletedAt();
        return entity;
    }

    public Buy toDomain(BuyEntity entity) {
        return Buy.create(BuyId.of(entity.id), entity.number, entity.name,
                OrganizationId.of(entity.organizationId), entity.createdAt, entity.updatedAt);
    }

    public static final BuyMapper INSTANCE = new BuyMapper();

    private BuyMapper() {
    }
}
