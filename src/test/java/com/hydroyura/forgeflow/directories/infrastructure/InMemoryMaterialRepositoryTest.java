package com.hydroyura.forgeflow.directories.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryMaterialRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryMaterialRepository repository = new InMemoryMaterialRepository();

    private static Material material(OrganizationId org, String number, String name) {
        return Material.create(MaterialId.generate(), number, name, StandardType.GOST, "ГОСТ 1050-2013", "кг",
                org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveMaterialOfOrganization() {
        Material material = material(orgA, "45", "Сталь 45");
        repository.save(material);

        assertThat(repository.findById(material.getId(), orgA)).contains(material);
        assertThat(repository.findById(material.getId(), orgB)).isEmpty();

        material.markDeleted();
        repository.save(material);
        assertThat(repository.findById(material.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveMaterialsOfOrganizationSortedByNumber() {
        Material second = material(orgA, "2", "Второй");
        Material first = material(orgA, "1", "Первый");
        Material other = material(orgB, "3", "Чужой");
        Material deleted = material(orgA, "0", "Удалённый");
        deleted.markDeleted();
        repository.save(second);
        repository.save(first);
        repository.save(other);
        repository.save(deleted);

        List<Material> result = repository.findAll(orgA);

        assertThat(result).extracting(Material::getNumber).containsExactly("1", "2");
    }

    @Test
    void existsByNumber_checksOnlyActiveMaterialsOfOrganization() {
        Material material = material(orgA, "45", "Сталь 45");
        repository.save(material);

        assertThat(repository.existsByNumber("45", orgA)).isTrue();
        assertThat(repository.existsByNumber("45", orgB)).isFalse();
        assertThat(repository.existsByNumber("99", orgA)).isFalse();

        material.markDeleted();
        repository.save(material);
        assertThat(repository.existsByNumber("45", orgA)).isFalse();
    }
}
