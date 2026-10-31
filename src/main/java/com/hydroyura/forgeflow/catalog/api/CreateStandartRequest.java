package com.hydroyura.forgeflow.catalog.api;

/**
 * Входной контракт создания стандартного изделия. Системные поля не передаются
 * клиентом.
 */
public record CreateStandartRequest(String number, String name) {
}
