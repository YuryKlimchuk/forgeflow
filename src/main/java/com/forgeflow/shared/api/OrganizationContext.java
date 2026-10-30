package com.forgeflow.shared.api;

import java.util.Optional;

/**
 * Read-only контракт получения текущей организации.
 *
 * <p>Бизнес-модули зависят только от этого интерфейса и не знают механизма
 * хранения (ThreadLocal, request scope и т.п.). Установка и очистка значения
 * выполняются инфраструктурой, а не бизнес-кодом.</p>
 */
public interface OrganizationContext {

    /**
     * Текущая организация, если она определена для текущего запроса.
     */
    Optional<OrganizationId> currentOrganizationId();

    /**
     * Текущая организация; выбрасывает исключение, если контекст отсутствует.
     */
    OrganizationId requiredOrganizationId();
}
