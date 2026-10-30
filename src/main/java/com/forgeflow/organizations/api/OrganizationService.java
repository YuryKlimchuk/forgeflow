package com.forgeflow.organizations.api;

import java.util.List;

/**
 * Публичный контракт операций над организациями, владельцами и участниками.
 *
 * <p>Действующий пользователь (subject) определяется серверно и не
 * передаётся клиентом; клиентские идентификаторы пользователя и роли
 * для авторизации не используются.</p>
 */
public interface OrganizationService {

    /**
     * Регистрирует текущего пользователя как владельца без организации.
     */
    void registerCurrentUser();

    /**
     * Создаёт организацию и связывает с ней текущего владельца без организации.
     */
    OrganizationView createOrganization(String name);

    /**
     * Возвращает организацию, если текущий пользователь её владелец или участник.
     */
    OrganizationView getOrganization(OrganizationId organizationId);

    /**
     * Переименовывает организацию; доступно только владельцу.
     */
    void renameOrganization(OrganizationId organizationId, String newName);

    /**
     * Добавляет пользователя участником организации; доступно только владельцу.
     */
    void addMember(OrganizationId organizationId, UserId userId);

    /**
     * Возвращает участников организации; доступно только владельцу.
     */
    List<UserId> listMembers(OrganizationId organizationId);

    /**
     * Удаляет участника организации; доступно только владельцу.
     */
    void removeMember(OrganizationId organizationId, UserId userId);

    /**
     * Возвращает принадлежность текущего пользователя.
     */
    Affiliation getAffiliation();
}
