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
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.catalog.domain.vzk.VzkRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.AssemblyJpaRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.BuyJpaRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.JpaAssemblyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.JpaBuyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.JpaPartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.JpaStandartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.JpaVzkRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.PartJpaRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.StandartJpaRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.persistence.VzkJpaRepository;
import com.hydroyura.forgeflow.directories.application.MaterialApplicationService;
import com.hydroyura.forgeflow.directories.application.MaterialMapper;
import com.hydroyura.forgeflow.directories.application.SortamentApplicationService;
import com.hydroyura.forgeflow.directories.application.SortamentMapper;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import com.hydroyura.forgeflow.directories.infrastructure.persistence.JpaMaterialRepository;
import com.hydroyura.forgeflow.directories.infrastructure.persistence.JpaSortamentRepository;
import com.hydroyura.forgeflow.directories.infrastructure.persistence.MaterialJpaRepository;
import com.hydroyura.forgeflow.directories.infrastructure.persistence.SortamentJpaRepository;
import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettingsProvider;
import com.hydroyura.forgeflow.organizations.application.MembershipCoordinator;
import com.hydroyura.forgeflow.organizations.application.OrganizationApplicationService;
import com.hydroyura.forgeflow.organizations.application.OrganizationSettingsProviderAdapter;
import com.hydroyura.forgeflow.organizations.application.SubjectProvider;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.SecurityContextSubjectProvider;
import com.hydroyura.forgeflow.organizations.infrastructure.SystemClock;
import com.hydroyura.forgeflow.organizations.infrastructure.persistence.JpaMembershipCoordinator;
import com.hydroyura.forgeflow.organizations.infrastructure.persistence.JpaOrganizationRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.persistence.JpaOwnerRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.persistence.OrganizationJpaRepository;
import com.hydroyura.forgeflow.organizations.infrastructure.persistence.OwnerJpaRepository;
import com.hydroyura.forgeflow.planning.application.PlanApplicationService;
import com.hydroyura.forgeflow.planning.application.PlanMapper;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.planning.infrastructure.persistence.JpaPlanRepository;
import com.hydroyura.forgeflow.planning.infrastructure.persistence.PlanItemJpaRepository;
import com.hydroyura.forgeflow.planning.infrastructure.persistence.PlanJpaRepository;
import com.hydroyura.forgeflow.security.api.AuthenticatedUserProvider;
import com.hydroyura.forgeflow.security.application.OrganizationContextResolver;
import com.hydroyura.forgeflow.security.infrastructure.OrganizationContextLifecycle;
import com.hydroyura.forgeflow.security.infrastructure.ThreadLocalOrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.specifications.application.SpecificationApplicationService;
import com.hydroyura.forgeflow.specifications.application.SpecificationMapper;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import com.hydroyura.forgeflow.specifications.domain.SubstitutionRepository;
import com.hydroyura.forgeflow.specifications.infrastructure.persistence.JpaSpecificationRepository;
import com.hydroyura.forgeflow.specifications.infrastructure.persistence.JpaSubstitutionRepository;
import com.hydroyura.forgeflow.specifications.infrastructure.persistence.SpecificationRowJpaRepository;
import com.hydroyura.forgeflow.specifications.infrastructure.persistence.SubstitutionJpaRepository;
import com.hydroyura.forgeflow.technology.api.EquipmentService;
import com.hydroyura.forgeflow.technology.api.TechnologyService;
import com.hydroyura.forgeflow.technology.application.EquipmentApplicationService;
import com.hydroyura.forgeflow.technology.application.EquipmentMapper;
import com.hydroyura.forgeflow.technology.application.TechnologyApplicationService;
import com.hydroyura.forgeflow.technology.application.TechnologyMapper;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessRepository;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessService;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
import com.hydroyura.forgeflow.technology.infrastructure.persistence.EquipmentJpaRepository;
import com.hydroyura.forgeflow.technology.infrastructure.persistence.JpaEquipmentRepository;
import com.hydroyura.forgeflow.technology.infrastructure.persistence.JpaTechnologyProcessRepository;
import com.hydroyura.forgeflow.technology.infrastructure.persistence.TechnologyProcessJpaRepository;
import com.hydroyura.forgeflow.technology.infrastructure.persistence.TechnologyProcessOperationJpaRepository;
import com.hydroyura.forgeflow.web.AssemblyController;
import com.hydroyura.forgeflow.web.BuyController;
import com.hydroyura.forgeflow.web.StandartController;
import com.hydroyura.forgeflow.web.VzkController;
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
    public PartRepository partRepository(PartJpaRepository repository) {
        return new JpaPartRepository(repository);
    }

    @Bean
    public AssemblyRepository assemblyRepository(AssemblyJpaRepository repository) {
        return new JpaAssemblyRepository(repository);
    }

    @Bean
    public BuyRepository buyRepository(BuyJpaRepository repository) {
        return new JpaBuyRepository(repository);
    }

    @Bean
    public StandartRepository standartRepository(StandartJpaRepository repository) {
        return new JpaStandartRepository(repository);
    }

    @Bean
    public VzkRepository vzkRepository(VzkJpaRepository repository) {
        return new JpaVzkRepository(repository);
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

    // --- Organizations ---

    @Bean
    public SubjectProvider subjectProvider() {
        return new SecurityContextSubjectProvider();
    }

    @Bean
    public OwnerRepository ownerRepository(OwnerJpaRepository repository) {
        return new JpaOwnerRepository(repository);
    }

    @Bean
    public OrganizationRepository organizationRepository(OrganizationJpaRepository repository) {
        return new JpaOrganizationRepository(repository);
    }

    @Bean
    public OrganizationSettingsProvider organizationSettingsProvider(
            OrganizationRepository organizationRepository) {
        return new OrganizationSettingsProviderAdapter(organizationRepository);
    }

    @Bean
    public MembershipCoordinator membershipCoordinator(
            OwnerRepository owners,
            OrganizationRepository organizations) {
        return new JpaMembershipCoordinator(owners, organizations);
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
            ThreadLocalOrganizationContext context,
            OrganizationService organizationService) {
        return new OrganizationContextLifecycle(userProvider, resolver, context, organizationService);
    }

    // --- Application services ---

    @Bean
    public PartApplicationService partService(
            PartRepository partRepository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            PartMapper partMapper,
            java.time.Clock systemClock) {
        return new PartApplicationService(partRepository, organizationContext, settingsProvider, partMapper, systemClock);
    }

    @Bean
    public AssemblyApplicationService assemblyService(
            AssemblyRepository assemblyRepository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            AssemblyMapper assemblyMapper,
            java.time.Clock systemClock) {
        return new AssemblyApplicationService(assemblyRepository, organizationContext, settingsProvider, assemblyMapper, systemClock);
    }

    @Bean
    public BuyApplicationService buyService(
            BuyRepository buyRepository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            BuyMapper buyMapper,
            java.time.Clock systemClock) {
        return new BuyApplicationService(buyRepository, organizationContext, settingsProvider, buyMapper, systemClock);
    }

    @Bean
    public StandartApplicationService standartService(
            StandartRepository standartRepository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            StandartMapper standartMapper,
            java.time.Clock systemClock) {
        return new StandartApplicationService(standartRepository, organizationContext, settingsProvider, standartMapper, systemClock);
    }

    @Bean
    public VzkApplicationService vzkService(
            VzkRepository vzkRepository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            VzkMapper vzkMapper,
            java.time.Clock systemClock) {
        return new VzkApplicationService(vzkRepository, organizationContext, settingsProvider, vzkMapper, systemClock);
    }

    // --- Specifications ---

    @Bean
    public SpecificationRepository specificationRepository(SpecificationRowJpaRepository repository) {
        return new JpaSpecificationRepository(repository);
    }

    @Bean
    public SubstitutionRepository substitutionRepository(SubstitutionJpaRepository repository) {
        return new JpaSubstitutionRepository(repository);
    }

    @Bean
    public SpecificationMapper specificationMapper() {
        return new SpecificationMapper();
    }

    @Bean
    public SpecificationApplicationService specificationService(
            SpecificationRepository specificationRepository,
            SubstitutionRepository substitutionRepository,
            OrganizationContext organizationContext,
            AssemblyService assemblyService,
            PartService partService,
            BuyService buyService,
            StandartService standartService,
            VzkService vzkService,
            SpecificationMapper specificationMapper) {
        return new SpecificationApplicationService(
                specificationRepository, substitutionRepository, organizationContext, assemblyService,
                partService, buyService, standartService, vzkService, specificationMapper);
    }

    // --- Planning ---

    @Bean
    public PlanRepository planRepository(PlanJpaRepository repository, PlanItemJpaRepository itemRepository) {
        return new JpaPlanRepository(repository, itemRepository);
    }

    @Bean
    public PlanMapper planMapper() {
        return new PlanMapper();
    }

    @Bean
    public PlanApplicationService planService(
            PlanRepository planRepository,
            OrganizationContext organizationContext,
            AssemblyService assemblyService,
            SpecificationService specificationService,
            PlanMapper planMapper) {
        return new PlanApplicationService(
                planRepository, organizationContext, assemblyService, specificationService, planMapper);
    }

    // --- Technology ---

    @Bean
    public TechnologyProcessRepository technologyProcessRepository(
            TechnologyProcessJpaRepository repository,
            TechnologyProcessOperationJpaRepository operationRepository) {
        return new JpaTechnologyProcessRepository(repository, operationRepository);
    }

    @Bean
    public EquipmentRepository equipmentRepository(EquipmentJpaRepository repository) {
        return new JpaEquipmentRepository(repository);
    }

    @Bean
    public TechnologyMapper technologyMapper() {
        return new TechnologyMapper();
    }

    @Bean
    public EquipmentMapper equipmentMapper() {
        return new EquipmentMapper();
    }

    @Bean
    public TechnologyProcessService technologyProcessService(
            TechnologyProcessRepository technologyProcessRepository) {
        return new TechnologyProcessService(technologyProcessRepository);
    }

    @Bean
    public TechnologyApplicationService technologyService(
            TechnologyProcessRepository technologyProcessRepository,
            TechnologyProcessService technologyProcessService,
            OrganizationContext organizationContext,
            PartService partService,
            EquipmentRepository equipmentRepository,
            TechnologyMapper technologyMapper,
            java.time.Clock systemClock) {
        return new TechnologyApplicationService(
                technologyProcessRepository, technologyProcessService, organizationContext,
                partService, equipmentRepository, technologyMapper, systemClock);
    }

    @Bean
    public EquipmentApplicationService equipmentService(
            EquipmentRepository equipmentRepository,
            OrganizationContext organizationContext,
            EquipmentMapper equipmentMapper,
            java.time.Clock systemClock) {
        return new EquipmentApplicationService(equipmentRepository, organizationContext, equipmentMapper, systemClock);
    }

    // --- Directories ---

    @Bean
    public MaterialRepository materialRepository(MaterialJpaRepository repository) {
        return new JpaMaterialRepository(repository);
    }

    @Bean
    public SortamentRepository sortamentRepository(SortamentJpaRepository repository) {
        return new JpaSortamentRepository(repository);
    }

    @Bean
    public MaterialMapper materialMapper() {
        return new MaterialMapper();
    }

    @Bean
    public SortamentMapper sortamentMapper() {
        return new SortamentMapper();
    }

    @Bean
    public MaterialApplicationService materialService(
            MaterialRepository materialRepository,
            OrganizationContext organizationContext,
            MaterialMapper materialMapper,
            java.time.Clock systemClock) {
        return new MaterialApplicationService(materialRepository, organizationContext, materialMapper, systemClock);
    }

    @Bean
    public SortamentApplicationService sortamentService(
            SortamentRepository sortamentRepository,
            OrganizationContext organizationContext,
            SortamentMapper sortamentMapper,
            java.time.Clock systemClock) {
        return new SortamentApplicationService(sortamentRepository, organizationContext, sortamentMapper, systemClock);
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
}
