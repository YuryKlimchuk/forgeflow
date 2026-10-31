package com.hydroyura.forgeflow.organizations.infrastructure;

import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.application.MembershipCoordinator;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * In-memory координатор: проверка занятости пользователя по владельцам и
 * участникам и атомарная критическая секция для конкурентных операций.
 */
public class InMemoryMembershipCoordinator implements MembershipCoordinator {

    private final OwnerRepository owners;
    private final OrganizationRepository organizations;
    private final ReentrantLock lock = new ReentrantLock();

    public InMemoryMembershipCoordinator(OwnerRepository owners, OrganizationRepository organizations) {
        this.owners = Objects.requireNonNull(owners, "owners must not be null");
        this.organizations = Objects.requireNonNull(organizations, "organizations must not be null");
    }

    @Override
    public boolean isUserIdOccupied(UserId userId) {
        return owners.findByUserId(userId).isPresent()
                || organizations.findByMemberUserId(userId).isPresent();
    }

    @Override
    public <T> T atomic(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
