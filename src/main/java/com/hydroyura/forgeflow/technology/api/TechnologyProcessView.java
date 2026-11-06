package com.hydroyura.forgeflow.technology.api;

import com.hydroyura.forgeflow.catalog.api.PartId;
import java.time.Instant;
import java.util.List;

/**
 * Представление технологического процесса для чтения.
 */
public record TechnologyProcessView(
        TechnologyProcessId id,
        String number,
        String name,
        PartId partId,
        boolean defaultProcess,
        Instant createdAt,
        Instant updatedAt,
        List<OperationView> operations) {
}
