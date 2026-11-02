package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Owner;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * База интеграционных тестов: поднимает один общий PostgreSQL через
 * Testcontainers (singleton) и применяет Flyway-миграции. Демо-организация и
 * владелец {@code demo-user} создаются здесь, т.к. {@code seed.sql} грузится
 * вручную и в тестах не выполняется.
 */
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("forgeflow")
            .withUsername("forgeflow")
            .withPassword("forgeflow");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static final UUID DEMO_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final String DEMO_USER_ID = "demo-user";
    protected static final String USERS_ISSUER = "http://localhost:8081/realms/forgeflow-users";

    protected static RequestPostProcessor demoUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.issuer(USERS_ISSUER).subject(DEMO_USER_ID));
    }

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    OwnerRepository ownerRepository;

    @BeforeEach
    void ensureDemoOrganization() {
        OrganizationId organizationId = OrganizationId.of(DEMO_ORGANIZATION_ID);
        if (organizationRepository.findById(organizationId).isEmpty()) {
            organizationRepository.save(Organization.create(organizationId, "Демо организация", Instant.now()));
        }
        UserId userId = UserId.of(DEMO_USER_ID);
        if (ownerRepository.findByUserId(userId).isEmpty()) {
            Owner owner = Owner.create(OwnerId.generate(), userId);
            owner.bind(organizationId);
            ownerRepository.save(owner);
        }
    }
}
