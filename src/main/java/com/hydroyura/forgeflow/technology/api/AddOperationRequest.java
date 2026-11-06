package com.hydroyura.forgeflow.technology.api;

/**
 * Входной контракт добавления операции в технологический процесс.
 */
public record AddOperationRequest(int number, String name) {
}
