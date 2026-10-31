package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.catalog.application.PartApplicationService;
import com.hydroyura.forgeflow.catalog.application.PartMapper;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryPartRepository;
import com.hydroyura.forgeflow.organizations.application.MembershipCoordinator;
import com.hydroyura.forgeflow.organizations.application.OrganizationApplicationService;
import com.hydroyura.forgeflow.organizations.application.SubjectProvider;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.InMemoryMembershipCoordinator;
import com.hydroyura.forgeflow.organizations.infrastructure.InMemoryOrganizationRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.InMemoryOwnerRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.MutableSubjectProvider;
import com.hydroyura.forgeflow.organizations.infrastructure.SystemClock;
import com.hydroyura.forgeflow.security.api.AuthenticatedUserProvider;
import com.hydroyura.forgeflow.security.application.OrganizationContextResolver;
import com.hydroyura.forgeflow.security.infrastructure.OrganizationContextLifecycle;
import com.hydroyura.forgeflow.security.infrastructure.StubAuthenticatedUserProvider;
import com.hydroyura.forgeflow.security.infrastructure.ThreadLocalOrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root: собирает бины из портов и адаптеров доменных модулей,
 * не загрязняя сами модули Spring-аннотациями.
 */
@Configuration
public class WebConfig {

    // --- Catalog ---

    @Bean
    public PartRepository partRepository() {
        return new InMemoryPartRepository();
    }

    @Bean
    public PartMapper partMapper() {
        return PartMapper.INSTANCE;
    }

    @Bean
    public java.time.Clock systemClock() {
        return java.time.Clock.systemUTC();
    }

    // --- Security ---

    @Bean
    public ThreadLocalOrganizationContext threadLocalOrganizationContext() {
        return new ThreadLocalOrganizationContext();
    }

    @Bean
    public AuthenticatedUserProvider authenticatedUserProvider() {
        return new StubAuthenticatedUserProvider();
    }

    // --- Organizations ---

    @Bean
    public MutableSubjectProvider subjectProvider() {
        return new MutableSubjectProvider();
    }

    @Bean
    public InMemoryOwnerRepository ownerRepository() {
        return new InMemoryOwnerRepository();
    }

    @Bean
    public InMemoryOrganizationRepository organizationRepository() {
        return new InMemoryOrganizationRepository();
    }

    @Bean
    public MembershipCoordinator membershipCoordinator(
            OwnerRepository owners,
            OrganizationRepository organizations) {
        return new InMemoryMembershipCoordinator(owners, organizations);
    }

    @Bean
    public com.hydroyura.forgeflow.organizations.application.Clock organizationsClock() {
        return new SystemClock();
    }

    @Bean
    public OrganizationApplicationService organizationService(
            SubjectProvider subjectProvider,
            com.hydroyura.forgeflow.organizations.application.Clock clock,
            OwnerRepository owners,
            OrganizationRepository organizations,
            MembershipCoordinator coordinator) {
        return new OrganizationApplicationService(subjectProvider, clock, owners, organizations, coordinator);
    }

    @Bean
    public OrganizationContextResolver organizationContextResolver(
            OrganizationApplicationService organizationService) {
        return new OrganizationContextResolver(organizationService);
    }

    @Bean
    public OrganizationContextLifecycle organizationContextLifecycle(
            AuthenticatedUserProvider userProvider,
            OrganizationContextResolver resolver,
            ThreadLocalOrganizationContext context) {
        return new OrganizationContextLifecycle(userProvider, resolver, context);
    }

    // --- Application services ---

    @Bean
    public PartApplicationService partService(
            PartRepository partRepository,
            OrganizationContext organizationContext,
            PartMapper partMapper,
            java.time.Clock systemClock) {
        return new PartApplicationService(partRepository, organizationContext, partMapper, systemClock);
    }

    // --- Web ---

    @Bean
    public TenantContextFilter tenantContextFilter(OrganizationContextLifecycle lifecycle) {
        return new TenantContextFilter(lifecycle);
    }
}
