package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "part")
public class PartEntity extends CatalogEntity {
}
