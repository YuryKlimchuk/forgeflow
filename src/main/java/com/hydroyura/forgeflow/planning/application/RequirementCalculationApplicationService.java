package com.hydroyura.forgeflow.planning.application;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationService;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationView;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationNotFoundException;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationRepository;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculator;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import com.hydroyura.forgeflow.planning.domain.RequirementSource;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link RequirementCalculationService}: запуск расчёта, чтение
 * истории и одного расчёта в контексте текущей организации.
 */
@Transactional
public class RequirementCalculationApplicationService implements RequirementCalculationService {

    private final RequirementCalculationRepository repository;
    private final PlanRepository planRepository;
    private final OrganizationContext organizationContext;
    private final RequirementCalculator calculator;
    private final RequirementSource source;
    private final Clock clock;
    private final RequirementCalculationMapper mapper;

    public RequirementCalculationApplicationService(
            RequirementCalculationRepository repository,
            PlanRepository planRepository,
            OrganizationContext organizationContext,
            RequirementCalculator calculator,
            RequirementSource source,
            Clock clock,
            RequirementCalculationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public RequirementCalculationView calculate(PlanId planId) {
        Objects.requireNonNull(planId, "planId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Plan plan = planRepository.findById(organizationId, planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        List<RequirementItem> items = calculator.calculate(plan, source);
        RequirementCalculation calculation = RequirementCalculation.create(
                RequirementCalculationId.generate(),
                organizationId,
                planId,
                clock.instant(),
                items);
        repository.save(calculation);
        return mapper.toView(calculation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequirementCalculationView> list(PlanId planId) {
        Objects.requireNonNull(planId, "planId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requirePlan(organizationId, planId);
        return repository.findByPlanId(organizationId, planId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RequirementCalculationView get(PlanId planId, RequirementCalculationId calculationId) {
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(calculationId, "calculationId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requirePlan(organizationId, planId);
        RequirementCalculation calculation = repository
                .findById(organizationId, planId, calculationId)
                .orElseThrow(() -> new RequirementCalculationNotFoundException(calculationId));
        return mapper.toView(calculation);
    }

    private void requirePlan(OrganizationId organizationId, PlanId planId) {
        planRepository.findById(organizationId, planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));
    }
}
