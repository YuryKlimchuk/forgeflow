package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA-модель организации: отделена от доменного агрегата {@code Organization}.
 */
@Entity
@Table(name = "organization")
public class OrganizationEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "name")
    public String name;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "part_number_pattern")
    public String partNumberPattern;

    @Column(name = "assembly_number_pattern")
    public String assemblyNumberPattern;

    @Column(name = "buy_number_pattern")
    public String buyNumberPattern;

    @Column(name = "standart_number_pattern")
    public String standartNumberPattern;

    @Column(name = "vzk_number_pattern")
    public String vzkNumberPattern;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_member", joinColumns = @JoinColumn(name = "organization_id"))
    @Column(name = "user_id")
    public Set<String> memberUserIds = new LinkedHashSet<>();
}
