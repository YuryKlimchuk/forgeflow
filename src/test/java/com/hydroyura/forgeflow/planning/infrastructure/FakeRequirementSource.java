package com.hydroyura.forgeflow.planning.infrastructure;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.domain.RequirementSource;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory фейк {@link RequirementSource} для тестов доменного алгоритма и
 * application-сервиса.
 */
public class FakeRequirementSource implements RequirementSource {

    private final Map<CatalogItemReference, ItemDescriptor> descriptors = new HashMap<>();
    private final Map<AssemblyId, List<SourceRow>> rowsByAssembly = new HashMap<>();

    public FakeRequirementSource descriptor(CatalogItemType type, UUID id, String number, String name) {
        descriptors.put(new CatalogItemReference(type, id), new ItemDescriptor(number, name));
        return this;
    }

    public FakeRequirementSource assembly(AssemblyId id, String number, String name) {
        return descriptor(CatalogItemType.ASSEMBLY, id.value(), number, name);
    }

    public FakeRequirementSource rows(AssemblyId assemblyId, SourceRow... rows) {
        rowsByAssembly.put(assemblyId, Arrays.asList(rows));
        return this;
    }

    @Override
    public List<SourceRow> rows(AssemblyId assemblyId) {
        return rowsByAssembly.getOrDefault(assemblyId, List.of());
    }

    @Override
    public Optional<ItemDescriptor> descriptor(CatalogItemReference item) {
        return Optional.ofNullable(descriptors.get(item));
    }

    public static SourceRow row(CatalogItemType type, UUID id, int quantity) {
        return new SourceRow(new CatalogItemReference(type, id), quantity);
    }
}
