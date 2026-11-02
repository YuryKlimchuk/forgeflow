package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;

/**
 * Преобразование доменного агрегата {@code Organization} и JPA-модели.
 */
public final class OrganizationMapper {

    public OrganizationEntity toEntity(Organization organization) {
        OrganizationEntity entity = new OrganizationEntity();
        entity.id = organization.getId().value();
        entity.name = organization.getName();
        entity.createdAt = organization.getCreatedAt();
        entity.updatedAt = organization.getUpdatedAt();
        OrganizationSettings settings = organization.getSettings();
        entity.partNumberPattern = settings.partNumberPattern();
        entity.assemblyNumberPattern = settings.assemblyNumberPattern();
        entity.buyNumberPattern = settings.buyNumberPattern();
        entity.standartNumberPattern = settings.standartNumberPattern();
        entity.vzkNumberPattern = settings.vzkNumberPattern();
        for (var member : organization.getMembers()) {
            entity.memberUserIds.add(member.userId().value());
        }
        return entity;
    }

    public Organization toDomain(OrganizationEntity entity) {
        return Organization.reconstitute(
                OrganizationId.of(entity.id),
                entity.name,
                entity.createdAt,
                entity.updatedAt,
                toSettings(entity),
                entity.memberUserIds.stream().map(UserId::of).toList());
    }

    public OrganizationSettings toSettings(OrganizationEntity entity) {
        return new OrganizationSettings(
                entity.partNumberPattern,
                entity.assemblyNumberPattern,
                entity.buyNumberPattern,
                entity.standartNumberPattern,
                entity.vzkNumberPattern);
    }

    public static final OrganizationMapper INSTANCE = new OrganizationMapper();

    private OrganizationMapper() {
    }
}
