package com.hydroyura.forgeflow.planning.api;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Представление элемента плана: сборка и количество.
 */
public record PlanItemView(AssemblyId assemblyId, int quantity) {
}
