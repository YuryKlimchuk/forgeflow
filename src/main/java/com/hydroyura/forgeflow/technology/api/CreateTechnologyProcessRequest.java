package com.hydroyura.forgeflow.technology.api;

import com.hydroyura.forgeflow.catalog.api.PartId;

/**
 * Входной контракт создания технологического процесса. Идентификатор,
 * организация и временные метки являются системными и не передаются клиентом.
 */
public record CreateTechnologyProcessRequest(String number, String name, PartId partId) {
}
