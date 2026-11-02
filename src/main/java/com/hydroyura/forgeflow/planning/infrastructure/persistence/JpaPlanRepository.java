package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaPlanRepository implements PlanRepository {

    private final PlanJpaRepository repository;
    private final PlanItemJpaRepository itemRepository;
    private final PlanMapper mapper = PlanMapper.INSTANCE;

    public JpaPlanRepository(PlanJpaRepository repository, PlanItemJpaRepository itemRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository must not be null");
    }

    @Override
    public void save(Plan plan) {
        repository.save(mapper.toEntity(plan));
        repository.flush();
        UUID planId = plan.getId().value();
        itemRepository.deleteByPlanId(planId);
        itemRepository.flush();
        itemRepository.saveAll(mapper.toItemEntities(plan));
    }

    @Override
    public Optional<Plan> findById(OrganizationId organizationId, PlanId id) {
        return repository.findByOrganizationIdAndId(organizationId.value(), id.value())
                .map(entity -> mapper.toDomain(entity, itemRepository.findByPlanIdOrderByAssemblyIdAsc(id.value())));
    }

    @Override
    public List<Plan> findAll(OrganizationId organizationId) {
        return repository.findByOrganizationIdOrderByNameAsc(organizationId.value()).stream()
                .map(entity -> mapper.toDomain(entity, itemRepository.findByPlanIdOrderByAssemblyIdAsc(entity.id)))
                .toList();
    }

    @Override
    public boolean existsByName(OrganizationId organizationId, String name) {
        return repository.existsByOrganizationIdAndName(organizationId.value(), name);
    }
}
