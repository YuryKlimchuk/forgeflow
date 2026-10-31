package com.hydroyura.forgeflow.planning.api;

/**
 * Запрос создания плана.
 */
public record CreatePlanRequest(String name, int year, int month) {
}
