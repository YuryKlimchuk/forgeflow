package com.hydroyura.forgeflow.organizations.api;

import com.hydroyura.forgeflow.shared.api.OrganizationId;

/**
 * Порт чтения настроек организации по её идентификатору. Публикуется модулем
 * {@code organizations} для потребителей из других модулей (например, каталога).
 */
@FunctionalInterface
public interface OrganizationSettingsProvider {

    /**
     * Возвращает настройки организации; для организации без явной конфигурации
     * возвращаются настройки без ограничений.
     */
    OrganizationSettings findSettings(OrganizationId organizationId);
}
