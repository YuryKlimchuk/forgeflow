package com.hydroyura.forgeflow.planning.api;

import java.util.List;

/**
 * Представление плана.
 */
public record PlanView(PlanId id, String name, int year, int month, List<PlanItemView> items) {
}
