package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.infrastructure.MutableSubjectProvider;
import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.Realms;
import com.hydroyura.forgeflow.security.infrastructure.StubAuthenticatedUserProvider;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Временные демо-данные для запуска без Keycloak: создаёт демо-пользователя и
 * организацию и возвращает её идентификатор. Будет удалён после интеграции
 * Keycloak.
 */
@Configuration
public class DemoDataBootstrap {

    @Bean
    public OrganizationId demoOrganizationId(
            StubAuthenticatedUserProvider userProvider,
            MutableSubjectProvider subjectProvider,
            OrganizationService organizationService) {
        UserId userId = UserId.of("demo-user");
        subjectProvider.set(userId);
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));
        organizationService.registerCurrentUser();
        return organizationService.createOrganization("Демо организация").id();
    }
}
