package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA-модель владельца организации.
 */
@Entity
@Table(name = "owner")
public class OwnerEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "user_id")
    public String userId;

    @Column(name = "organization_id")
    public UUID organizationId;
}
