package com.hydroyura.forgeflow.planning.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.planning.api.AddPlanItemRequest;
import com.hydroyura.forgeflow.planning.api.ChangePlanItemQuantityRequest;
import com.hydroyura.forgeflow.planning.api.CreatePlanRequest;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.PlanItemView;
import com.hydroyura.forgeflow.planning.api.PlanService;
import com.hydroyura.forgeflow.planning.api.PlanView;
import com.hydroyura.forgeflow.planning.domain.DuplicatePlanNameException;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanItem;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link PlanService}: управление планами и расчёт сводной
 * потребности. Текущая организация берётся из {@link OrganizationContext}.
 */
@Transactional
public class PlanApplicationService implements PlanService {

    private final PlanRepository repository;
    private final OrganizationContext organizationContext;
    private final AssemblyService assemblyService;
    private final SpecificationService specificationService;
    private final PlanMapper mapper;

    public PlanApplicationService(
            PlanRepository repository,
            OrganizationContext organizationContext,
            AssemblyService assemblyService,
            SpecificationService specificationService,
            PlanMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService must not be null");
        this.specificationService = Objects.requireNonNull(specificationService, "specificationService must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlanView create(CreatePlanRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByName(organizationId, request.name())) {
            throw new DuplicatePlanNameException(request.name());
        }
        Plan plan = Plan.create(PlanId.generate(), organizationId, request.name(), request.year(), request.month());
        repository.save(plan);
        return mapper.toView(plan);
    }

    @Override
    public PlanView get(PlanId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Plan plan = repository.findById(organizationId, id)
                .orElseThrow(() -> new PlanNotFoundException(id));
        return mapper.toView(plan);
    }

    @Override
    public List<PlanView> list() {
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public PlanItemView addItem(PlanId id, AddPlanItemRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Plan plan = repository.findById(organizationId, id)
                .orElseThrow(() -> new PlanNotFoundException(id));
        assemblyService.get(request.assemblyId());
        plan.addItem(request.assemblyId(), request.quantity());
        repository.save(plan);
        return new PlanItemView(request.assemblyId(), request.quantity());
    }

    @Override
    public void changeQuantity(PlanId id, AssemblyId assemblyId, ChangePlanItemQuantityRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Plan plan = repository.findById(organizationId, id)
                .orElseThrow(() -> new PlanNotFoundException(id));
        plan.changeQuantity(assemblyId, request.quantity());
        repository.save(plan);
    }

    @Override
    public void removeItem(PlanId id, AssemblyId assemblyId) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Plan plan = repository.findById(organizationId, id)
                .orElseThrow(() -> new PlanNotFoundException(id));
        plan.removeItem(assemblyId);
        repository.save(plan);
    }

    @Override
    public List<FlatBomItem> flatten(PlanId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Plan plan = repository.findById(organizationId, id)
                .orElseThrow(() -> new PlanNotFoundException(id));

        Map<String, FlatBomItem> byNumber = new LinkedHashMap<>();
        for (PlanItem item : plan.items()) {
            for (FlatBomItem flat : specificationService.flatten(item.assemblyId())) {
                int total = Math.multiplyExact(flat.quantity(), item.quantity());
                byNumber.merge(flat.number(), new FlatBomItem(flat.number(), flat.name(), total),
                        (a, b) -> new FlatBomItem(a.number(), a.name(), Math.addExact(a.quantity(), b.quantity())));
            }
        }
        List<FlatBomItem> result = new ArrayList<>(byNumber.values());
        result.sort(Comparator.comparing(FlatBomItem::number));
        return result;
    }
}
