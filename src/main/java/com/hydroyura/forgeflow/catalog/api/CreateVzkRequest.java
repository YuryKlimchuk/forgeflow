package com.hydroyura.forgeflow.catalog.api;

/**
 * Входной контракт создания группы ВЗК. Системные поля не передаются клиентом.
 */
public record CreateVzkRequest(String number, String name) {
}
