package com.hydroyura.forgeflow.catalog.api;

/**
 * Входной контракт создания сборочной единицы. Системные поля не передаются
 * клиентом.
 */
public record CreateAssemblyRequest(String number, String name) {
}
