package com.hydroyura.forgeflow.directories.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.directories.api.CreateMaterialRequest;
import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.MaterialView;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.api.UpdateMaterialRequest;
import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.MaterialNotFoundException;
import com.hydroyura.forgeflow.directories.infrastructure.InMemoryMaterialRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryMaterialRepository repository = new InMemoryMaterialRepository();
    private final MaterialApplicationService service = serviceFor(orgA);

    private static CreateMaterialRequest request(String number, String name) {
        return new CreateMaterialRequest(number, name, StandardType.GOST, "ГОСТ 1050-2013", "кг");
    }

    @Test
    void create_returnsViewWithSystemFieldsAndIsVisibleInReads() {
        MaterialView created = service.create(request("45", "Сталь 45"));

        assertThat(created.id()).isNotNull();
        assertThat(created.number()).isEqualTo("45");
        assertThat(created.name()).isEqualTo("Сталь 45");
        assertThat(created.standardType()).isEqualTo(StandardType.GOST);
        assertThat(created.standard()).isEqualTo("ГОСТ 1050-2013");
        assertThat(created.unit()).isEqualTo("кг");
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(service.get(created.id())).isEqualTo(created);
        assertThat(service.list()).containsExactly(created);
    }

    @Test
    void create_duplicateNumberInSameOrganization_rejects() {
        service.create(request("45", "Сталь 45"));

        assertThatThrownBy(() -> service.create(request("45", "Другая сталь")))
                .isInstanceOf(DuplicateMaterialNumberException.class);
        assertThat(repository.findAll(orgA)).hasSize(1);
    }

    @Test
    void create_sameNumberInDifferentOrganizations_allowed() {
        service.create(request("45", "Сталь 45"));
        MaterialApplicationService otherOrgService = serviceFor(orgB);

        MaterialView other = otherOrgService.create(request("45", "Другая сталь"));

        assertThat(service.list()).extracting(MaterialView::number).containsExactly("45");
        assertThat(otherOrgService.list()).extracting(MaterialView::number).containsExactly("45");
        assertThat(otherOrgService.get(other.id())).isEqualTo(other);
    }

    @Test
    void create_blankFieldOrNullStandardType_rejects() {
        assertThatThrownBy(() -> service.create(request("   ", "Сталь 45")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> service.create(request("45", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> service.create(new CreateMaterialRequest("45", "Сталь 45", null, "ГОСТ 1050-2013", "кг")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standardType");
        assertThatThrownBy(() -> service.create(new CreateMaterialRequest("45", "Сталь 45", StandardType.GOST, "   ", "кг")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standard");
        assertThatThrownBy(() -> service.create(new CreateMaterialRequest("45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");
    }

    @Test
    void create_usesOrganizationFromContext() {
        MaterialView created = service.create(request("45", "Сталь 45"));

        assertThat(repository.findById(created.id(), orgA)).isPresent();
        assertThat(repository.findById(created.id(), orgB)).isEmpty();
    }

    @Test
    void get_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.get(MaterialId.generate()))
                .isInstanceOf(MaterialNotFoundException.class);
    }

    @Test
    void get_deleted_throwsNotFound() {
        MaterialView created = service.create(request("45", "Сталь 45"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(MaterialNotFoundException.class);
    }

    @Test
    void get_foreignMaterial_throwsNotFound() {
        MaterialApplicationService otherOrgService = serviceFor(orgB);
        MaterialView foreign = otherOrgService.create(request("45", "Чужой"));

        assertThatThrownBy(() -> service.get(foreign.id()))
                .isInstanceOf(MaterialNotFoundException.class);
    }

    @Test
    void list_returnsSortedByNumber() {
        service.create(request("2", "Второй"));
        service.create(request("1", "Первый"));

        assertThat(service.list()).extracting(MaterialView::number).containsExactly("1", "2");
    }

    @Test
    void list_excludesDeletedAndForeign() {
        service.create(request("1", "Свой"));
        serviceFor(orgB).create(request("3", "Чужой"));
        MaterialView deleted = service.create(request("0", "Удалённый"));
        service.delete(deleted.id());

        assertThat(service.list()).extracting(MaterialView::number).containsExactly("1");
    }

    @Test
    void update_changesMutableFieldsAndKeepsNumber() {
        MaterialView created = service.create(request("45", "Сталь 45"));

        service.update(created.id(), new UpdateMaterialRequest("Новое имя", StandardType.DIN, "DIN EN 10088-2", "м"));

        MaterialView updated = service.get(created.id());
        assertThat(updated.name()).isEqualTo("Новое имя");
        assertThat(updated.standardType()).isEqualTo(StandardType.DIN);
        assertThat(updated.standard()).isEqualTo("DIN EN 10088-2");
        assertThat(updated.unit()).isEqualTo("м");
        assertThat(updated.number()).isEqualTo("45");
        assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void update_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.update(MaterialId.generate(),
                new UpdateMaterialRequest("Имя", StandardType.GOST, "ГОСТ 1050-2013", "кг")))
                .isInstanceOf(MaterialNotFoundException.class);
    }

    @Test
    void update_invalidValues_rejectsAndKeepsState() {
        MaterialView created = service.create(request("45", "Сталь 45"));

        assertThatThrownBy(() -> service.update(created.id(),
                new UpdateMaterialRequest("   ", StandardType.GOST, "ГОСТ 1050-2013", "кг")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.get(created.id()).name()).isEqualTo("Сталь 45");
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        MaterialView created = service.create(request("45", "Сталь 45"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(MaterialNotFoundException.class);
        assertThat(service.list()).isEmpty();
    }

    @Test
    void delete_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.delete(MaterialId.generate()))
                .isInstanceOf(MaterialNotFoundException.class);
    }

    @Test
    void create_sameNumberAfterSoftDelete_succeeds() {
        MaterialView first = service.create(request("45", "Сталь 45"));
        service.delete(first.id());

        MaterialView second = service.create(request("45", "Сталь 45 новая"));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(service.list()).extracting(MaterialView::number).containsExactly("45");
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        MaterialApplicationService noContext = serviceFor(null);

        assertThatThrownBy(() -> noContext.list())
                .isInstanceOf(OrganizationContextNotAvailableException.class);
    }

    private MaterialApplicationService serviceFor(OrganizationId organizationId) {
        return new MaterialApplicationService(
                repository,
                fakeContext(organizationId),
                new MaterialMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static OrganizationContext fakeContext(OrganizationId id) {
        return new OrganizationContext() {
            @Override
            public Optional<OrganizationId> currentOrganizationId() {
                return Optional.ofNullable(id);
            }

            @Override
            public OrganizationId requiredOrganizationId() {
                if (id == null) {
                    throw new OrganizationContextNotAvailableException("no organization context");
                }
                return id;
            }
        };
    }
}
