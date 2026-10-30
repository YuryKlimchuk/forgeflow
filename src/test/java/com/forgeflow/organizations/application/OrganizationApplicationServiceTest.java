package com.forgeflow.organizations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.organizations.api.Affiliation;
import com.forgeflow.organizations.api.OrganizationId;
import com.forgeflow.organizations.api.OrganizationView;
import com.forgeflow.organizations.api.UserId;
import com.forgeflow.organizations.infrastructure.InMemoryMembershipCoordinator;
import com.forgeflow.organizations.infrastructure.InMemoryOrganizationRepository;
import com.forgeflow.organizations.infrastructure.InMemoryOwnerRepository;
import com.forgeflow.organizations.infrastructure.MutableSubjectProvider;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-02T00:00:00Z");

    private MutableSubjectProvider subjectProvider;
    private MutableClock clock;
    private InMemoryOwnerRepository owners;
    private InMemoryOrganizationRepository organizations;
    private InMemoryMembershipCoordinator coordinator;
    private OrganizationApplicationService service;

    @BeforeEach
    void setUp() {
        subjectProvider = new MutableSubjectProvider();
        clock = new MutableClock(NOW);
        owners = new InMemoryOwnerRepository();
        organizations = new InMemoryOrganizationRepository();
        coordinator = new InMemoryMembershipCoordinator(owners, organizations);
        service = new OrganizationApplicationService(subjectProvider, clock, owners, organizations, coordinator);
    }

    @Test
    void registerOwnerWithoutOrganization() {
        subjectProvider.set(UserId.of("owner-1"));
        service.registerCurrentUser();

        assertThat(service.getAffiliation()).isEqualTo(new Affiliation.None());
    }

    @Test
    void registerSameUserTwiceRejected() {
        subjectProvider.set(UserId.of("owner-1"));
        service.registerCurrentUser();

        assertThatThrownBy(service::registerCurrentUser)
                .isInstanceOf(UserAlreadyOccupiedException.class);
    }

    @Test
    void createOrganizationBindsOwner() {
        subjectProvider.set(UserId.of("owner-1"));
        service.registerCurrentUser();

        OrganizationView view = service.createOrganization("Acme");

        assertThat(view.name()).isEqualTo("Acme");
        assertThat(view.createdAt()).isEqualTo(NOW);
        assertThat(service.getAffiliation()).isEqualTo(new Affiliation.OwnerOf(view.id()));
    }

    @Test
    void createOrganizationWithoutRegistrationRejected() {
        subjectProvider.set(UserId.of("owner-1"));

        assertThatThrownBy(() -> service.createOrganization("Acme"))
                .isInstanceOf(OwnerRequiredException.class);
    }

    @Test
    void createSecondOrganizationRejected() {
        subjectProvider.set(UserId.of("owner-1"));
        service.registerCurrentUser();
        service.createOrganization("Acme");

        assertThatThrownBy(() -> service.createOrganization("Other"))
                .isInstanceOf(UserAlreadyOccupiedException.class);
    }

    @Test
    void ownerAndMemberReadOwnOrganization() {
        OrganizationId orgId = createOrganizationWithOwnerAndMember("owner-1", "member-1", "Acme");

        subjectProvider.set(UserId.of("owner-1"));
        assertThat(service.getOrganization(orgId).name()).isEqualTo("Acme");

        subjectProvider.set(UserId.of("member-1"));
        assertThat(service.getOrganization(orgId).name()).isEqualTo("Acme");
    }

    @Test
    void readForeignOrganizationRejected() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");

        subjectProvider.set(UserId.of("stranger"));
        assertThatThrownBy(() -> service.getOrganization(orgId))
                .isInstanceOf(OrganizationAccessDeniedException.class);
    }

    @Test
    void renameByOwnerUpdatesNameAndUpdatedAt() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");

        subjectProvider.set(UserId.of("owner-1"));
        clock.set(LATER);
        service.renameOrganization(orgId, "Acme Inc");

        OrganizationView view = service.getOrganization(orgId);
        assertThat(view.name()).isEqualTo("Acme Inc");
        assertThat(view.updatedAt()).isEqualTo(LATER);
        assertThat(view.createdAt()).isEqualTo(NOW);
    }

    @Test
    void renameByMemberRejected() {
        OrganizationId orgId = createOrganizationWithOwnerAndMember("owner-1", "member-1", "Acme");

        subjectProvider.set(UserId.of("member-1"));
        assertThatThrownBy(() -> service.renameOrganization(orgId, "X"))
                .isInstanceOf(OwnerRequiredException.class);
    }

    @Test
    void addMemberByOwner() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");

        subjectProvider.set(UserId.of("owner-1"));
        service.addMember(orgId, UserId.of("member-1"));

        assertThat(service.listMembers(orgId)).containsExactly(UserId.of("member-1"));

        subjectProvider.set(UserId.of("member-1"));
        assertThat(service.getAffiliation()).isEqualTo(new Affiliation.MemberOf(orgId));
    }

    @Test
    void addDuplicateMemberRejected() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");
        subjectProvider.set(UserId.of("owner-1"));
        service.addMember(orgId, UserId.of("member-1"));

        assertThatThrownBy(() -> service.addMember(orgId, UserId.of("member-1")))
                .isInstanceOf(UserAlreadyOccupiedException.class);
    }

    @Test
    void addOwnerAsMemberRejected() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");

        subjectProvider.set(UserId.of("owner-1"));
        assertThatThrownBy(() -> service.addMember(orgId, UserId.of("owner-1")))
                .isInstanceOf(UserAlreadyOccupiedException.class);
    }

    @Test
    void addMemberByMemberRejected() {
        OrganizationId orgId = createOrganizationWithOwnerAndMember("owner-1", "member-1", "Acme");

        subjectProvider.set(UserId.of("member-1"));
        assertThatThrownBy(() -> service.addMember(orgId, UserId.of("member-2")))
                .isInstanceOf(OwnerRequiredException.class);
    }

    @Test
    void memberListExcludesOwner() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");
        subjectProvider.set(UserId.of("owner-1"));
        service.addMember(orgId, UserId.of("member-1"));

        assertThat(service.listMembers(orgId)).containsExactly(UserId.of("member-1"));
    }

    @Test
    void listMembersByMemberRejected() {
        OrganizationId orgId = createOrganizationWithOwnerAndMember("owner-1", "member-1", "Acme");

        subjectProvider.set(UserId.of("member-1"));
        assertThatThrownBy(() -> service.listMembers(orgId))
                .isInstanceOf(OwnerRequiredException.class);
    }

    @Test
    void removeMemberByOwner() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");
        subjectProvider.set(UserId.of("owner-1"));
        service.addMember(orgId, UserId.of("member-1"));

        service.removeMember(orgId, UserId.of("member-1"));

        assertThat(service.listMembers(orgId)).isEmpty();
    }

    @Test
    void removeOwnerRejected() {
        OrganizationId orgId = createOrganizationWithOwner("owner-1", "Acme");

        subjectProvider.set(UserId.of("owner-1"));
        assertThatThrownBy(() -> service.removeMember(orgId, UserId.of("owner-1")))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void removeMemberByMemberRejected() {
        OrganizationId orgId = createOrganizationWithOwnerAndMember("owner-1", "member-1", "Acme");

        subjectProvider.set(UserId.of("member-1"));
        assertThatThrownBy(() -> service.removeMember(orgId, UserId.of("member-1")))
                .isInstanceOf(OwnerRequiredException.class);
    }

    @Test
    void affiliationForUnknownUserIsNone() {
        subjectProvider.set(UserId.of("unknown"));

        assertThat(service.getAffiliation()).isEqualTo(new Affiliation.None());
    }

    @Test
    void tenantIsolationAcrossOrganizations() {
        OrganizationId orgA = createOrganizationWithOwner("owner-a", "A");
        subjectProvider.set(UserId.of("owner-b"));
        service.registerCurrentUser();
        OrganizationId orgB = service.createOrganization("B").id();
        service.addMember(orgB, UserId.of("member-b"));

        // владелец организации A не имеет доступа к организации B
        subjectProvider.set(UserId.of("owner-a"));
        assertThatThrownBy(() -> service.getOrganization(orgB))
                .isInstanceOf(OrganizationAccessDeniedException.class);
        assertThatThrownBy(() -> service.renameOrganization(orgB, "X"))
                .isInstanceOf(OwnerRequiredException.class);
        assertThatThrownBy(() -> service.listMembers(orgB))
                .isInstanceOf(OwnerRequiredException.class);
        assertThatThrownBy(() -> service.addMember(orgB, UserId.of("intruder")))
                .isInstanceOf(OwnerRequiredException.class);

        // участник организации B не имеет доступа к организации A
        subjectProvider.set(UserId.of("member-b"));
        assertThatThrownBy(() -> service.getOrganization(orgA))
                .isInstanceOf(OrganizationAccessDeniedException.class);
    }

    private OrganizationId createOrganizationWithOwner(String ownerUser, String name) {
        subjectProvider.set(UserId.of(ownerUser));
        service.registerCurrentUser();
        return service.createOrganization(name).id();
    }

    private OrganizationId createOrganizationWithOwnerAndMember(String ownerUser, String memberUser, String name) {
        OrganizationId orgId = createOrganizationWithOwner(ownerUser, name);
        subjectProvider.set(UserId.of(ownerUser));
        service.addMember(orgId, UserId.of(memberUser));
        return orgId;
    }

    static final class MutableClock implements Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant now() {
            return instant;
        }
    }
}
