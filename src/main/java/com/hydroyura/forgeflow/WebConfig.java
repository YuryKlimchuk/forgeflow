package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.catalog.application.AssemblyApplicationService;
import com.hydroyura.forgeflow.catalog.application.AssemblyMapper;
import com.hydroyura.forgeflow.catalog.application.BuyApplicationService;
import com.hydroyura.forgeflow.catalog.application.BuyMapper;
import com.hydroyura.forgeflow.catalog.application.PartApplicationService;
import com.hydroyura.forgeflow.catalog.application.PartMapper;
import com.hydroyura.forgeflow.catalog.application.StandartApplicationService;
import com.hydroyura.forgeflow.catalog.application.StandartMapper;
import com.hydroyura.forgeflow.catalog.application.VzkApplicationService;
import com.hydroyura.forgeflow.catalog.application.VzkMapper;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.catalog.domain.vzk.VzkRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryAssemblyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryBuyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryPartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryStandartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryVzkRepository;
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
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.web.AssemblyController;
import com.hydroyura.forgeflow.web.BuyController;
import com.hydroyura.forgeflow.web.StandartController;
import com.hydroyura.forgeflow.web.VzkController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

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
    public AssemblyRepository assemblyRepository() {
        return new InMemoryAssemblyRepository();
    }

    @Bean
    public BuyRepository buyRepository() {
        return new InMemoryBuyRepository();
    }

    @Bean
    public StandartRepository standartRepository() {
        return new InMemoryStandartRepository();
    }

    @Bean
    public VzkRepository vzkRepository() {
        return new InMemoryVzkRepository();
    }

    @Bean
    public PartMapper partMapper() {
        return PartMapper.INSTANCE;
    }

    @Bean
    public AssemblyMapper assemblyMapper() {
        return AssemblyMapper.INSTANCE;
    }

    @Bean
    public BuyMapper buyMapper() {
        return BuyMapper.INSTANCE;
    }

    @Bean
    public StandartMapper standartMapper() {
        return StandartMapper.INSTANCE;
    }

    @Bean
    public VzkMapper vzkMapper() {
        return VzkMapper.INSTANCE;
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
    public StubAuthenticatedUserProvider authenticatedUserProvider() {
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

    @Bean
    public AssemblyApplicationService assemblyService(
            AssemblyRepository assemblyRepository,
            OrganizationContext organizationContext,
            AssemblyMapper assemblyMapper,
            java.time.Clock systemClock) {
        return new AssemblyApplicationService(assemblyRepository, organizationContext, assemblyMapper, systemClock);
    }

    @Bean
    public BuyApplicationService buyService(
            BuyRepository buyRepository,
            OrganizationContext organizationContext,
            BuyMapper buyMapper,
            java.time.Clock systemClock) {
        return new BuyApplicationService(buyRepository, organizationContext, buyMapper, systemClock);
    }

    @Bean
    public StandartApplicationService standartService(
            StandartRepository standartRepository,
            OrganizationContext organizationContext,
            StandartMapper standartMapper,
            java.time.Clock systemClock) {
        return new StandartApplicationService(standartRepository, organizationContext, standartMapper, systemClock);
    }

    @Bean
    public VzkApplicationService vzkService(
            VzkRepository vzkRepository,
            OrganizationContext organizationContext,
            VzkMapper vzkMapper,
            java.time.Clock systemClock) {
        return new VzkApplicationService(vzkRepository, organizationContext, vzkMapper, systemClock);
    }

    // --- Web ---

    @Bean
    public AssemblyController assemblyController(AssemblyService assemblyService) {
        return new AssemblyController(assemblyService);
    }

    @Bean
    public BuyController buyController(BuyService buyService) {
        return new BuyController(buyService);
    }

    @Bean
    public StandartController standartController(StandartService standartService) {
        return new StandartController(standartService);
    }

    @Bean
    public VzkController vzkController(VzkService vzkService) {
        return new VzkController(vzkService);
    }

    @Bean
    public TenantContextFilter tenantContextFilter(OrganizationContextLifecycle lifecycle) {
        return new TenantContextFilter(lifecycle);
    }

    // --- Seed ---

    @Bean
    public SeedProperties seedProperties(
            @Value("${forgeflow.seed.enabled:true}") boolean enabled,
            @Value("${forgeflow.seed.data-file:seed-data/catalog.json}") String dataFile) {
        return new SeedProperties(enabled, dataFile);
    }

    @Bean
    public DataSeeder dataSeeder(
            PartRepository partRepository,
            AssemblyRepository assemblyRepository,
            OrganizationId demoOrganizationId,
            SeedProperties seedProperties,
            ObjectMapper objectMapper) {
        return new DataSeeder(partRepository, assemblyRepository, demoOrganizationId, seedProperties, objectMapper);
    }
}
