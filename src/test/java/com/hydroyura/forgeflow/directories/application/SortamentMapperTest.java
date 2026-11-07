package com.hydroyura.forgeflow.directories.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.SortamentView;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SortamentMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    private final SortamentMapper mapper = new SortamentMapper();

    @Test
    void toView_mapsAllReadableFieldsAndOmitsOrganizationAndDeletedAt() {
        SortamentId id = SortamentId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        Sortament sortament = Sortament.create(
                id, "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                organizationId, CREATED, UPDATED);

        SortamentView view = mapper.toView(sortament);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("Круг В-30");
        assertThat(view.name()).isEqualTo("Круг горячекатаный В-30");
        assertThat(view.standardType()).isEqualTo(StandardType.GOST);
        assertThat(view.standard()).isEqualTo("ГОСТ 2590-2006");
        assertThat(view.unit()).isEqualTo("мм");
        assertThat(view.createdAt()).isEqualTo(CREATED);
        assertThat(view.updatedAt()).isEqualTo(UPDATED);
        assertThat(view.getClass().getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("organizationId", "deletedAt");
    }
}
