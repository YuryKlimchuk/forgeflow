package com.hydroyura.forgeflow.catalog.api;

/**
 * Входной контракт создания покупного изделия. Системные поля не передаются
 * клиентом.
 */
public record CreateBuyRequest(String number, String name) {
}
