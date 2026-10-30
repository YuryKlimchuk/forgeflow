package com.forgeflow.organizations.application;

import com.forgeflow.organizations.api.Affiliation;
import com.forgeflow.organizations.api.OrganizationId;
import com.forgeflow.organizations.api.OrganizationService;
import com.forgeflow.organizations.api.OrganizationView;
import com.forgeflow.organizations.api.OwnerId;
import com.forgeflow.organizations.api.UserId;
import com.forgeflow.organizations.domain.Membership;
import com.forgeflow.organizations.domain.Organization;
import com.forgeflow.organizations.domain.OrganizationRepository;
import com.forgeflow.organizations.domain.Owner;
import com.forgeflow.organizations.domain.OwnerRepository;
import java.util.List;
import java.util.Objects;

/**
 * Реализация публичных операций организаций. Действующий пользователь
 * определяется через {@link SubjectProvider}, а конкурентные операции
 * координируются через {@link MembershipCoordinator}.
 */
public class OrganizationApplicationService implements OrganizationService {

    private final SubjectProvider subjectProvider;
    private final Clock clock;
    private final OwnerRepository ownerRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipCoordinator membershipCoordinator;

    public OrganizationApplicationService(
            SubjectProvider subjectProvider,
            Clock clock,
            OwnerRepository ownerRepository,
            OrganizationRepository organizationRepository,
            MembershipCoordinator membershipCoordinator) {
        this.subjectProvider = Objects.requireNonNull(subjectProvider, "subjectProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.ownerRepository = Objects.requireNonNull(ownerRepository, "ownerRepository must not be null");
        this.organizationRepository = Objects.requireNonNull(organizationRepository, "organizationRepository must not be null");
        this.membershipCoordinator = Objects.requireNonNull(membershipCoordinator, "membershipCoordinator must not be null");
    }

    @Override
    public void registerCurrentUser() {
        UserId subject = subjectProvider.currentSubject();
        membershipCoordinator.atomic(() -> {
            if (membershipCoordinator.isUserIdOccupied(subject)) {
                throw new UserAlreadyOccupiedException("User is already registered");
            }
            ownerRepository.save(Owner.create(OwnerId.generate(), subject));
            return null;
        });
    }

    @Override
    public OrganizationView createOrganization(String name) {
        UserId subject = subjectProvider.currentSubject();
        return membershipCoordinator.atomic(() -> {
            Owner owner = ownerRepository.findByUserId(subject)
                    .orElseThrow(() -> new OwnerRequiredException("Only a registered owner can create an organization"));
            if (owner.getOrganizationId() != null) {
                throw new UserAlreadyOccupiedException("Owner already belongs to an organization");
            }
            OrganizationId id = OrganizationId.generate();
            Organization organization = Organization.create(id, name, clock.now());
            owner.bind(id);
            organizationRepository.save(organization);
            ownerRepository.save(owner);
            return toView(organization);
        });
    }

    @Override
    public OrganizationView getOrganization(OrganizationId organizationId) {
        UserId subject = subjectProvider.currentSubject();
        Organization organization = requireOrganization(organizationId);
        if (!isOwner(subject, organizationId) && !organization.hasMember(subject)) {
            throw new OrganizationAccessDeniedException("User has no access to the organization");
        }
        return toView(organization);
    }

    @Override
    public void renameOrganization(OrganizationId organizationId, String newName) {
        UserId subject = subjectProvider.currentSubject();
        membershipCoordinator.atomic(() -> {
            requireOwner(subject, organizationId);
            Organization organization = requireOrganization(organizationId);
            organization.rename(newName, clock.now());
            organizationRepository.save(organization);
            return null;
        });
    }

    @Override
    public void addMember(OrganizationId organizationId, UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserId subject = subjectProvider.currentSubject();
        membershipCoordinator.atomic(() -> {
            requireOwner(subject, organizationId);
            if (membershipCoordinator.isUserIdOccupied(userId)) {
                throw new UserAlreadyOccupiedException("User already belongs to an organization");
            }
            Organization organization = requireOrganization(organizationId);
            organization.addMember(userId);
            organizationRepository.save(organization);
            return null;
        });
    }

    @Override
    public List<UserId> listMembers(OrganizationId organizationId) {
        UserId subject = subjectProvider.currentSubject();
        requireOwner(subject, organizationId);
        Organization organization = requireOrganization(organizationId);
        return organization.getMembers().stream()
                .map(Membership::userId)
                .toList();
    }

    @Override
    public void removeMember(OrganizationId organizationId, UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserId subject = subjectProvider.currentSubject();
        membershipCoordinator.atomic(() -> {
            requireOwner(subject, organizationId);
            Organization organization = requireOrganization(organizationId);
            if (!organization.hasMember(userId)) {
                throw new MemberNotFoundException("User is not a member of the organization");
            }
            organization.removeMember(userId);
            organizationRepository.save(organization);
            return null;
        });
    }

    @Override
    public Affiliation getAffiliation() {
        UserId subject = subjectProvider.currentSubject();
        Owner owner = ownerRepository.findByUserId(subject).orElse(null);
        if (owner != null) {
            return owner.getOrganizationId() != null
                    ? new Affiliation.OwnerOf(owner.getOrganizationId())
                    : new Affiliation.None();
        }
        return organizationRepository.findByMemberUserId(subject)
                .<Affiliation>map(org -> new Affiliation.MemberOf(org.getId()))
                .orElseGet(Affiliation.None::new);
    }

    private Organization requireOrganization(OrganizationId organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));
    }

    private boolean isOwner(UserId subject, OrganizationId organizationId) {
        return ownerRepository.findByUserId(subject)
                .map(owner -> organizationId.equals(owner.getOrganizationId()))
                .orElse(false);
    }

    private void requireOwner(UserId subject, OrganizationId organizationId) {
        if (!isOwner(subject, organizationId)) {
            throw new OwnerRequiredException("Only the owner can perform this operation");
        }
    }

    private static OrganizationView toView(Organization organization) {
        return new OrganizationView(
                organization.getId(),
                organization.getName(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}
