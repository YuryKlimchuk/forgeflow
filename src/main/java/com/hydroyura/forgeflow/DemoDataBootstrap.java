package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.infrastructure.MutableSubjectProvider;
import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.Realms;
import com.hydroyura.forgeflow.security.infrastructure.StubAuthenticatedUserProvider;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Временная runtime-настройка стаб-аутентификации для запуска без Keycloak.
 * Демо-организация и членство создаются скриптом {@code seed.sql}, а не здесь.
 * Будет удалена после интеграции Keycloak.
 */
@Configuration
public class DemoDataBootstrap {

    /** Фиксированный UUID демо-организации, совпадает со значением в {@code seed.sql}. */
    private static final UUID DEMO_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Bean
    public OrganizationId demoOrganizationId(
            StubAuthenticatedUserProvider userProvider,
            MutableSubjectProvider subjectProvider) {
        UserId userId = UserId.of("demo-user");
        subjectProvider.set(userId);
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));
        return OrganizationId.of(DEMO_ORGANIZATION_ID);
    }
}
