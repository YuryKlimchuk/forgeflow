package com.hydroyura.forgeflow.specifications.api;

/**
 * Запрос копирования спецификации в новую сборку с явными номером и именем.
 */
public record CopySpecificationRequest(String number, String name) {
}
