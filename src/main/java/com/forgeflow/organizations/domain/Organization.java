package com.forgeflow.organizations.domain;

import com.forgeflow.organizations.api.OrganizationId;
import com.forgeflow.organizations.api.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Организация (tenant) — aggregate root собственных данных и коллекции
 * участников. Владелец хранится отдельно ({@link Owner}).
 */
@Entity
public class Organization {

    @Identity
    private final OrganizationId id;

    private String name;

    private final Instant createdAt;

    private Instant updatedAt;

    private final List<Membership> members = new ArrayList<>();

    private Organization(OrganizationId id, String name, Instant now) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = requireNotBlank(name);
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = this.createdAt;
    }

    public static Organization create(OrganizationId id, String name, Instant now) {
        return new Organization(id, name, now);
    }

    public void rename(String newName, Instant now) {
        this.name = requireNotBlank(newName);
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void addMember(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (hasMember(userId)) {
            throw new IllegalArgumentException("User is already a member of this organization");
        }
        members.add(new Membership(userId));
    }

    public void removeMember(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        boolean removed = members.removeIf(m -> m.userId().equals(userId));
        if (!removed) {
            throw new IllegalArgumentException("User is not a member of this organization");
        }
    }

    public boolean hasMember(UserId userId) {
        return members.stream().anyMatch(m -> m.userId().equals(userId));
    }

    public List<Membership> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public OrganizationId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireNotBlank(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return value;
    }
}
