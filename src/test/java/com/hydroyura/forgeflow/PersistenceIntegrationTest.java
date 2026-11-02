package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Owner;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.domain.Quantity;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import com.hydroyura.forgeflow.specifications.domain.Substitution;
import com.hydroyura.forgeflow.specifications.domain.SubstitutionRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Проверка persistence-адаптеров на настоящем PostgreSQL: round-trip,
 * soft-delete и tenant-изоляция.
 */
@Transactional
class PersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    PartRepository partRepository;

    @Autowired
    AssemblyRepository assemblyRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    OwnerRepository ownerRepository;

    @Autowired
    SpecificationRepository specificationRepository;

    @Autowired
    SubstitutionRepository substitutionRepository;

    @Autowired
    PlanRepository planRepository;

    @Test
    void partRoundTripTenantIsolationAndSoftDelete() {
        OrganizationId orgA = OrganizationId.of(DEMO_ORGANIZATION_ID);
        OrganizationId orgB = OrganizationId.generate();

        Part part = Part.create(PartId.generate(), "Д-9001", "Тестовая деталь", orgA, Instant.now(), Instant.now());
        partRepository.save(part);

        assertThat(partRepository.findById(part.getId(), orgA)).isPresent();
        assertThat(partRepository.findById(part.getId(), orgB)).isEmpty();
        assertThat(partRepository.findAll(orgA)).anyMatch(p -> p.getId().equals(part.getId()));
        assertThat(partRepository.findAll(orgB)).isEmpty();
        assertThat(partRepository.existsByNumber("Д-9001", orgA)).isTrue();
        assertThat(partRepository.existsByNumber("Д-9001", orgB)).isFalse();

        part.markDeleted();
        partRepository.save(part);

        assertThat(partRepository.findById(part.getId(), orgA)).isEmpty();
        assertThat(partRepository.existsByNumber("Д-9001", orgA)).isFalse();
    }

    @Test
    void organizationAndOwnerRoundTrip() {
        OrganizationId orgId = OrganizationId.generate();
        organizationRepository.save(Organization.create(orgId, "Орг-" + UUID.randomUUID(), Instant.now()));

        assertThat(organizationRepository.findById(orgId)).isPresent();

        UserId userId = UserId.of("user-" + UUID.randomUUID());
        Owner owner = Owner.create(OwnerId.generate(), userId);
        owner.bind(orgId);
        ownerRepository.save(owner);

        assertThat(ownerRepository.findByUserId(userId)).isPresent();
        assertThat(ownerRepository.findByUserId(userId).orElseThrow().getOrganizationId()).isEqualTo(orgId);
        assertThat(organizationRepository.findByMemberUserId(userId)).isEmpty();
    }

    @Test
    void specificationAndSubstitutionRoundTrip() {
        OrganizationId orgId = OrganizationId.of(DEMO_ORGANIZATION_ID);
        AssemblyId assemblyId = AssemblyId.generate();

        CatalogItemReference part = new CatalogItemReference(CatalogItemType.PART, UUID.randomUUID());
        Specification specification = Specification.create(orgId, assemblyId);
        specification.addRow(part, new Quantity(3));
        specificationRepository.save(specification);

        Specification loaded = specificationRepository.findById(orgId, assemblyId).orElseThrow();
        assertThat(loaded.rows()).hasSize(1);
        assertThat(loaded.rows().get(0).item()).isEqualTo(part);

        CatalogItemReference substitute = new CatalogItemReference(CatalogItemType.PART, UUID.randomUUID());
        Substitution substitution = Substitution.create(orgId, assemblyId, part);
        substitution.addSubstitute(substitute);
        substitutionRepository.save(substitution);

        Substitution loadedSubstitution = substitutionRepository.findById(orgId, assemblyId, part).orElseThrow();
        assertThat(loadedSubstitution.substitutes()).containsExactly(substitute);

        substitutionRepository.delete(orgId, assemblyId, part);
        assertThat(substitutionRepository.findById(orgId, assemblyId, part)).isEmpty();
    }

    @Test
    void planRoundTripAndTenantIsolation() {
        OrganizationId orgId = OrganizationId.of(DEMO_ORGANIZATION_ID);
        OrganizationId foreign = OrganizationId.generate();

        AssemblyId assemblyId = AssemblyId.generate();
        Plan plan = Plan.create(PlanId.generate(), orgId, "План-" + UUID.randomUUID(), 2026, 6);
        plan.addItem(assemblyId, 5);
        planRepository.save(plan);

        assertThat(planRepository.findById(orgId, plan.getId())).isPresent();
        assertThat(planRepository.findById(foreign, plan.getId())).isEmpty();
        assertThat(planRepository.existsByName(orgId, plan.getName())).isTrue();
        assertThat(planRepository.findAll(foreign)).isEmpty();

        Plan loaded = planRepository.findById(orgId, plan.getId()).orElseThrow();
        assertThat(loaded.items()).hasSize(1);
        assertThat(loaded.items().get(0).assemblyId()).isEqualTo(assemblyId);
    }
}
