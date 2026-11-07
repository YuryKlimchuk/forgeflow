package com.hydroyura.forgeflow.directories.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.directories.api.CreateSortamentRequest;
import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.SortamentView;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.api.UpdateSortamentRequest;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentNotFoundException;
import com.hydroyura.forgeflow.directories.infrastructure.InMemorySortamentRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SortamentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemorySortamentRepository repository = new InMemorySortamentRepository();
    private final SortamentApplicationService service = serviceFor(orgA);

    private static CreateSortamentRequest request(String number, String name) {
        return new CreateSortamentRequest(number, name, StandardType.GOST, "ГОСТ 2590-2006", "мм");
    }

    @Test
    void create_returnsViewWithSystemFieldsAndIsVisibleInReads() {
        SortamentView created = service.create(request("Круг В-30", "Круг горячекатаный В-30"));

        assertThat(created.id()).isNotNull();
        assertThat(created.number()).isEqualTo("Круг В-30");
        assertThat(created.name()).isEqualTo("Круг горячекатаный В-30");
        assertThat(created.standardType()).isEqualTo(StandardType.GOST);
        assertThat(created.standard()).isEqualTo("ГОСТ 2590-2006");
        assertThat(created.unit()).isEqualTo("мм");
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(service.get(created.id())).isEqualTo(created);
        assertThat(service.list()).containsExactly(created);
    }

    @Test
    void create_duplicateNumberInSameOrganization_rejects() {
        service.create(request("Круг В-30", "Круг"));

        assertThatThrownBy(() -> service.create(request("Круг В-30", "Другой круг")))
                .isInstanceOf(DuplicateSortamentNumberException.class);
        assertThat(repository.findAll(orgA)).hasSize(1);
    }

    @Test
    void create_sameNumberInDifferentOrganizations_allowed() {
        service.create(request("Круг В-30", "Круг"));
        SortamentApplicationService otherOrgService = serviceFor(orgB);

        SortamentView other = otherOrgService.create(request("Круг В-30", "Другой круг"));

        assertThat(service.list()).extracting(SortamentView::number).containsExactly("Круг В-30");
        assertThat(otherOrgService.list()).extracting(SortamentView::number).containsExactly("Круг В-30");
        assertThat(otherOrgService.get(other.id())).isEqualTo(other);
    }

    @Test
    void create_blankFieldOrNullStandardType_rejects() {
        assertThatThrownBy(() -> service.create(request("   ", "Круг")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> service.create(request("Круг В-30", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> service.create(new CreateSortamentRequest("Круг В-30", "Круг", null, "ГОСТ 2590-2006", "мм")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standardType");
        assertThatThrownBy(() -> service.create(new CreateSortamentRequest("Круг В-30", "Круг", StandardType.GOST, "   ", "мм")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standard");
        assertThatThrownBy(() -> service.create(new CreateSortamentRequest("Круг В-30", "Круг", StandardType.GOST, "ГОСТ 2590-2006", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");
    }

    @Test
    void create_usesOrganizationFromContext() {
        SortamentView created = service.create(request("Круг В-30", "Круг"));

        assertThat(repository.findById(created.id(), orgA)).isPresent();
        assertThat(repository.findById(created.id(), orgB)).isEmpty();
    }

    @Test
    void get_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.get(SortamentId.generate()))
                .isInstanceOf(SortamentNotFoundException.class);
    }

    @Test
    void get_deleted_throwsNotFound() {
        SortamentView created = service.create(request("Круг В-30", "Круг"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(SortamentNotFoundException.class);
    }

    @Test
    void get_foreignSortament_throwsNotFound() {
        SortamentApplicationService otherOrgService = serviceFor(orgB);
        SortamentView foreign = otherOrgService.create(request("Круг В-30", "Чужой"));

        assertThatThrownBy(() -> service.get(foreign.id()))
                .isInstanceOf(SortamentNotFoundException.class);
    }

    @Test
    void list_returnsSortedByNumber() {
        service.create(request("Лист", "Лист"));
        service.create(request("Круг", "Круг"));

        assertThat(service.list()).extracting(SortamentView::number).containsExactly("Круг", "Лист");
    }

    @Test
    void list_excludesDeletedAndForeign() {
        service.create(request("Круг", "Свой"));
        serviceFor(orgB).create(request("Труба", "Чужой"));
        SortamentView deleted = service.create(request("Уголок", "Удалённый"));
        service.delete(deleted.id());

        assertThat(service.list()).extracting(SortamentView::number).containsExactly("Круг");
    }

    @Test
    void update_changesMutableFieldsAndKeepsNumber() {
        SortamentView created = service.create(request("Круг В-30", "Круг"));

        service.update(created.id(), new UpdateSortamentRequest("Новое имя", StandardType.DIN, "DIN EN 10210", "см"));

        SortamentView updated = service.get(created.id());
        assertThat(updated.name()).isEqualTo("Новое имя");
        assertThat(updated.standardType()).isEqualTo(StandardType.DIN);
        assertThat(updated.standard()).isEqualTo("DIN EN 10210");
        assertThat(updated.unit()).isEqualTo("см");
        assertThat(updated.number()).isEqualTo("Круг В-30");
        assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void update_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.update(SortamentId.generate(),
                new UpdateSortamentRequest("Имя", StandardType.GOST, "ГОСТ 2590-2006", "мм")))
                .isInstanceOf(SortamentNotFoundException.class);
    }

    @Test
    void update_invalidValues_rejectsAndKeepsState() {
        SortamentView created = service.create(request("Круг В-30", "Круг"));

        assertThatThrownBy(() -> service.update(created.id(),
                new UpdateSortamentRequest("   ", StandardType.GOST, "ГОСТ 2590-2006", "мм")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.get(created.id()).name()).isEqualTo("Круг");
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        SortamentView created = service.create(request("Круг В-30", "Круг"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(SortamentNotFoundException.class);
        assertThat(service.list()).isEmpty();
    }

    @Test
    void delete_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.delete(SortamentId.generate()))
                .isInstanceOf(SortamentNotFoundException.class);
    }

    @Test
    void create_sameNumberAfterSoftDelete_succeeds() {
        SortamentView first = service.create(request("Круг В-30", "Круг"));
        service.delete(first.id());

        SortamentView second = service.create(request("Круг В-30", "Круг новый"));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(service.list()).extracting(SortamentView::number).containsExactly("Круг В-30");
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        SortamentApplicationService noContext = serviceFor(null);

        assertThatThrownBy(() -> noContext.list())
                .isInstanceOf(OrganizationContextNotAvailableException.class);
    }

    private SortamentApplicationService serviceFor(OrganizationId organizationId) {
        return new SortamentApplicationService(
                repository,
                fakeContext(organizationId),
                new SortamentMapper(),
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
