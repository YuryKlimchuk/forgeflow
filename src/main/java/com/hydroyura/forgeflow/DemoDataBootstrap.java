package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.infrastructure.MutableSubjectProvider;
import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.Realms;
import com.hydroyura.forgeflow.security.infrastructure.StubAuthenticatedUserProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Временные демо-данные для запуска без Keycloak: создаёт демо-пользователя и
 * организацию, чтобы REST-слой работал "из коробки". Будет удалён после
 * интеграции Keycloak.
 */
@Configuration
public class DemoDataBootstrap {

    @Bean
    CommandLineRunner demoData(
            StubAuthenticatedUserProvider userProvider,
            MutableSubjectProvider subjectProvider,
            OrganizationService organizationService) {
        return args -> {
            UserId userId = UserId.of("demo-user");
            subjectProvider.set(userId);
            userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));
            organizationService.registerCurrentUser();
            organizationService.createOrganization("Демо организация");
        };
    }
}
