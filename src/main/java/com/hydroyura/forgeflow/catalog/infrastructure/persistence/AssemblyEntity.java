package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "assembly")
public class AssemblyEntity extends CatalogEntity {
}
