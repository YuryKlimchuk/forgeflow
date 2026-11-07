package com.hydroyura.forgeflow.planning.api;

import java.time.Instant;
import java.util.List;

/**
 * Представление сохранённого расчёта потребностей.
 */
public record RequirementCalculationView(
        RequirementCalculationId id,
        PlanId planId,
        Instant executedAt,
        List<RequirementItemView> items) {
}
