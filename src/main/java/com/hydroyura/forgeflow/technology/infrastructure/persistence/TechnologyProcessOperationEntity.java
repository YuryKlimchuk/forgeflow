package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "technology_process_operation")
@IdClass(TechnologyProcessOperationId.class)
public class TechnologyProcessOperationEntity {

    @Id
    @Column(name = "process_id")
    public UUID processId;

    @Id
    @Column(name = "operation_number")
    public int operationNumber;

    @Column(name = "operation_name")
    public String operationName;

    @Column(name = "equipment_id")
    public UUID equipmentId;
}
