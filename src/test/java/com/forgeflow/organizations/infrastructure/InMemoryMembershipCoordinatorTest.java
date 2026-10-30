package com.forgeflow.organizations.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgeflow.shared.api.OrganizationId;
import com.forgeflow.organizations.api.OwnerId;
import com.forgeflow.organizations.api.UserId;
import com.forgeflow.organizations.application.UserAlreadyOccupiedException;
import com.forgeflow.organizations.domain.Organization;
import com.forgeflow.organizations.domain.Owner;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryMembershipCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void isUserIdOccupiedDetectsOwnerAndMember() {
        InMemoryOwnerRepository owners = new InMemoryOwnerRepository();
        InMemoryOrganizationRepository organizations = new InMemoryOrganizationRepository();
        InMemoryMembershipCoordinator coordinator =
                new InMemoryMembershipCoordinator(owners, organizations);

        UserId ownerUser = UserId.of("owner-1");
        UserId memberUser = UserId.of("member-1");
        UserId freeUser = UserId.of("free-1");

        assertThat(coordinator.isUserIdOccupied(freeUser)).isFalse();

        owners.save(Owner.create(OwnerId.generate(), ownerUser));
        assertThat(coordinator.isUserIdOccupied(ownerUser)).isTrue();

        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        organization.addMember(memberUser);
        organizations.save(organization);
        assertThat(coordinator.isUserIdOccupied(memberUser)).isTrue();
    }

    @Test
    void atomicReturnsValue() {
        InMemoryMembershipCoordinator coordinator = new InMemoryMembershipCoordinator(
                new InMemoryOwnerRepository(), new InMemoryOrganizationRepository());

        String result = coordinator.atomic(() -> "done");

        assertThat(result).isEqualTo("done");
    }

    @Test
    void concurrentRegistrationOfSameUserYieldsSingleSuccess() throws Exception {
        InMemoryOwnerRepository owners = new InMemoryOwnerRepository();
        InMemoryOrganizationRepository organizations = new InMemoryOrganizationRepository();
        InMemoryMembershipCoordinator coordinator =
                new InMemoryMembershipCoordinator(owners, organizations);

        UserId user = UserId.of("contended-user");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        Runnable register = () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                coordinator.atomic(() -> {
                    if (coordinator.isUserIdOccupied(user)) {
                        throw new UserAlreadyOccupiedException("taken");
                    }
                    owners.save(Owner.create(OwnerId.generate(), user));
                    return null;
                });
                success.incrementAndGet();
            } catch (UserAlreadyOccupiedException e) {
                rejected.incrementAndGet();
            }
        };

        pool.submit(register);
        pool.submit(register);
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(owners.findByUserId(user)).isPresent();
    }
}
