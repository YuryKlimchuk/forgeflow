package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Owner;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

/**
 * Преобразование доменного агрегата {@code Owner} и JPA-модели.
 */
public final class OwnerMapper {

    public OwnerEntity toEntity(Owner owner) {
        OwnerEntity entity = new OwnerEntity();
        entity.id = owner.getId().value();
        entity.userId = owner.getUserId().value();
        entity.organizationId = owner.getOrganizationId() == null ? null : owner.getOrganizationId().value();
        return entity;
    }

    public Owner toDomain(OwnerEntity entity) {
        Owner owner = Owner.create(OwnerId.of(entity.id), UserId.of(entity.userId));
        if (entity.organizationId != null) {
            owner.bind(OrganizationId.of(entity.organizationId));
        }
        return owner;
    }

    public static final OwnerMapper INSTANCE = new OwnerMapper();

    private OwnerMapper() {
    }
}
