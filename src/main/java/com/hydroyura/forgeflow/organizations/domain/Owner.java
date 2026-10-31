package com.hydroyura.forgeflow.organizations.domain;

import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import java.util.Objects;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Владелец организации. Создаётся при регистрации до создания организации,
 * после чего связывается с организацией отношением 1:1.
 */
@Entity
public class Owner {

    @Identity
    private final OwnerId id;

    private final UserId userId;

    private OrganizationId organizationId;

    private Owner(OwnerId id, UserId userId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    public static Owner create(OwnerId id, UserId userId) {
        return new Owner(id, userId);
    }

    /**
     * Связывает владельца с организацией; возможно только один раз.
     */
    public void bind(OrganizationId organizationId) {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        if (this.organizationId != null) {
            throw new IllegalStateException("Owner is already bound to an organization");
        }
        this.organizationId = organizationId;
    }

    public OwnerId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }
}
