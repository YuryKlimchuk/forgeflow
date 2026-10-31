package com.hydroyura.forgeflow.planning.api;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Запрос добавления сборки в план.
 */
public record AddPlanItemRequest(AssemblyId assemblyId, int quantity) {
}
