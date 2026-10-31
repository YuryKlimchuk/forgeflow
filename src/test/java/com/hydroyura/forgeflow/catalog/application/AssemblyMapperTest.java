package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AssemblyMapperTest {

    @Test
    void mapsAssemblyToView() {
        AssemblyId id = AssemblyId.generate();
        OrganizationId org = OrganizationId.generate();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        Assembly assembly = Assembly.create(id, "А-1", "Узел", org, created, updated);

        AssemblyView view = AssemblyMapper.INSTANCE.toView(assembly);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("А-1");
        assertThat(view.name()).isEqualTo("Узел");
        assertThat(view.createdAt()).isEqualTo(created);
        assertThat(view.updatedAt()).isEqualTo(updated);
    }
}
