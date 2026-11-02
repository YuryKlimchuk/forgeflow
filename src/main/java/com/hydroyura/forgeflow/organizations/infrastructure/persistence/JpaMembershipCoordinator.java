package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.application.MembershipCoordinator;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * JPA-координатор конкурентных операций: атомарность обеспечивается транзакцией
 * application-сервиса и ограничениями уникальности БД, а не JVM-блокировкой.
 */
public class JpaMembershipCoordinator implements MembershipCoordinator {

    private final OwnerRepository owners;
    private final OrganizationRepository organizations;

    public JpaMembershipCoordinator(OwnerRepository owners, OrganizationRepository organizations) {
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
        return action.get();
    }
}
