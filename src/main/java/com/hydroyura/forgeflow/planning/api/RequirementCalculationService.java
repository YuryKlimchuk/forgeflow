package com.hydroyura.forgeflow.planning.api;

import java.util.List;

/**
 * Публичный контракт расчёта потребностей производства.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface RequirementCalculationService {

    RequirementCalculationView calculate(PlanId planId);

    List<RequirementCalculationView> list(PlanId planId);

    RequirementCalculationView get(PlanId planId, RequirementCalculationId calculationId);
}
