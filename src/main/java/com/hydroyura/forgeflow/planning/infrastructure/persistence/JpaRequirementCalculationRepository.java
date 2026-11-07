package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationRepository;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaRequirementCalculationRepository implements RequirementCalculationRepository {

    private final RequirementCalculationJpaRepository repository;
    private final RequirementCalculationItemJpaRepository itemRepository;
    private final RequirementCalculationItemSourceJpaRepository sourceRepository;
    private final RequirementCalculationMapper mapper = RequirementCalculationMapper.INSTANCE;

    public JpaRequirementCalculationRepository(
            RequirementCalculationJpaRepository repository,
            RequirementCalculationItemJpaRepository itemRepository,
            RequirementCalculationItemSourceJpaRepository sourceRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository must not be null");
        this.sourceRepository = Objects.requireNonNull(sourceRepository, "sourceRepository must not be null");
    }

    @Override
    public void save(RequirementCalculation calculation) {
        repository.save(mapper.toEntity(calculation));
        repository.flush();

        UUID calculationId = calculation.getId().value();
        List<RequirementCalculationItemEntity> itemEntities = new ArrayList<>();
        List<RequirementCalculationItemSourceEntity> sourceEntities = new ArrayList<>();
        for (RequirementItem item : calculation.items()) {
            RequirementCalculationItemEntity itemEntity = mapper.toItemEntity(calculationId, item);
            itemEntities.add(itemEntity);
            for (var source : item.sources()) {
                sourceEntities.add(mapper.toSourceEntity(itemEntity.id, source));
            }
        }
        itemRepository.saveAll(itemEntities);
        itemRepository.flush();
        sourceRepository.saveAll(sourceEntities);
        sourceRepository.flush();
    }

    @Override
    public List<RequirementCalculation> findByPlanId(OrganizationId organizationId, PlanId planId) {
        return repository
                .findByOrganizationIdAndPlanIdOrderByExecutedAtDesc(organizationId.value(), planId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<RequirementCalculation> findById(
            OrganizationId organizationId,
            PlanId planId,
            RequirementCalculationId calculationId) {
        return repository
                .findByOrganizationIdAndPlanIdAndId(organizationId.value(), planId.value(), calculationId.value())
                .map(this::toDomain);
    }

    private RequirementCalculation toDomain(RequirementCalculationEntity entity) {
        List<RequirementCalculationItemEntity> itemEntities = itemRepository.findByCalculationId(entity.id);
        List<UUID> itemIds = itemEntities.stream().map(item -> item.id).toList();
        List<RequirementCalculationItemSourceEntity> sourceEntities = itemIds.isEmpty()
                ? List.of()
                : sourceRepository.findByCalculationItemIdIn(itemIds);
        return mapper.toDomain(entity, itemEntities, sourceEntities);
    }
}
