package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.Operation;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import java.util.List;

/**
 * Инфраструктурный маппер entity {@literal <->} domain. Восстановление агрегата
 * выполняется через {@link TechnologyProcess#restore} без потерь полей.
 */
public final class TechnologyMapper {

    public TechnologyProcessEntity toEntity(TechnologyProcess process) {
        TechnologyProcessEntity entity = new TechnologyProcessEntity();
        entity.id = process.getId().value();
        entity.number = process.getNumber();
        entity.name = process.getName();
        entity.partId = process.getPartId().value();
        entity.organizationId = process.getOrganizationId().value();
        entity.defaultProcess = process.isDefaultProcess();
        entity.createdAt = process.getCreatedAt();
        entity.updatedAt = process.getUpdatedAt();
        entity.deletedAt = process.getDeletedAt();
        return entity;
    }

    public List<TechnologyProcessOperationEntity> toOperationEntities(TechnologyProcess process) {
        return process.operations().stream()
                .map(operation -> {
                    TechnologyProcessOperationEntity entity = new TechnologyProcessOperationEntity();
                    entity.processId = process.getId().value();
                    entity.operationNumber = operation.number();
                    entity.operationName = operation.name();
                    return entity;
                })
                .toList();
    }

    public TechnologyProcess toDomain(TechnologyProcessEntity entity, List<TechnologyProcessOperationEntity> operations) {
        List<Operation> domainOperations = operations.stream()
                .map(operation -> new Operation(operation.operationNumber, operation.operationName))
                .toList();
        return TechnologyProcess.restore(
                TechnologyProcessId.of(entity.id),
                entity.number,
                entity.name,
                PartId.of(entity.partId),
                OrganizationId.of(entity.organizationId),
                entity.defaultProcess,
                entity.createdAt,
                entity.updatedAt,
                entity.deletedAt,
                domainOperations);
    }

    public static final TechnologyMapper INSTANCE = new TechnologyMapper();

    private TechnologyMapper() {
    }
}
