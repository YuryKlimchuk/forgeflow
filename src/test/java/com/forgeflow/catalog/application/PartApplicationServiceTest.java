package com.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.catalog.api.CreatePartRequest;
import com.forgeflow.catalog.api.PartFilter;
import com.forgeflow.catalog.api.PartId;
import com.forgeflow.catalog.api.PartView;
import com.forgeflow.catalog.domain.part.Part;
import com.forgeflow.catalog.domain.part.PartRepository;
import com.forgeflow.catalog.infrastructure.InMemoryPartRepository;
import com.forgeflow.shared.api.OrganizationContext;
import com.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartApplicationServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final InMemoryPartRepository repository = new InMemoryPartRepository();
    private final PartApplicationService service = new PartApplicationService(
            repository, fakeContext(orgId), PartMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void create_returnsViewWithSystemFields() {
        PartView view = service.create(new CreatePartRequest("P-1", "Деталь"));

        assertThat(view.id()).isNotNull();
        assertThat(view.number()).isEqualTo("P-1");
        assertThat(view.name()).isEqualTo("Деталь");
        assertThat(view.createdAt()).isEqualTo(FIXED);
        assertThat(view.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void create_rejectsDuplicateNumber() {
        service.create(new CreatePartRequest("P-1", "Деталь"));

        assertThatThrownBy(() -> service.create(new CreatePartRequest("P-1", "Другая")))
                .isInstanceOf(DuplicatePartNumberException.class);
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new CreatePartRequest("P-1", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void get_returnsActivePart() {
        PartView created = service.create(new CreatePartRequest("P-1", "Деталь"));

        PartView view = service.get(created.id());

        assertThat(view).isEqualTo(created);
    }

    @Test
    void get_throwsWhenMissing() {
        assertThatThrownBy(() -> service.get(PartId.generate()))
                .isInstanceOf(PartNotFoundException.class);
    }

    @Test
    void get_throwsWhenDeleted() {
        PartView created = service.create(new CreatePartRequest("P-1", "Деталь"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(PartNotFoundException.class);
    }

    @Test
    void list_returnsOnlyActiveSortedByNumber() {
        service.create(new CreatePartRequest("P-2", "Вторая"));
        service.create(new CreatePartRequest("P-1", "Первая"));
        PartView deleted = service.create(new CreatePartRequest("P-0", "Удалённая"));
        service.delete(deleted.id());

        List<PartView> result = service.list(new PartFilter());

        assertThat(result).extracting(PartView::number).containsExactly("P-1", "P-2");
    }

    @Test
    void rename_updatesName() {
        PartView created = service.create(new CreatePartRequest("P-1", "Деталь"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_throwsWhenMissing() {
        assertThatThrownBy(() -> service.rename(PartId.generate(), "Имя"))
                .isInstanceOf(PartNotFoundException.class);
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        PartView created = service.create(new CreatePartRequest("P-1", "Деталь"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(PartNotFoundException.class);
        assertThat(service.list(new PartFilter())).isEmpty();
    }

    @Test
    void delete_throwsWhenAlreadyDeletedOrMissing() {
        PartView created = service.create(new CreatePartRequest("P-1", "Деталь"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(PartNotFoundException.class);
        assertThatThrownBy(() -> service.delete(PartId.generate())).isInstanceOf(PartNotFoundException.class);
    }

    @Test
    void passesOrganizationIdFromContextToRepository() {
        RecordingPartRepository recording = new RecordingPartRepository();
        PartApplicationService recordingService = new PartApplicationService(
                recording, fakeContext(orgId), PartMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        recordingService.create(new CreatePartRequest("P-1", "Деталь"));
        recordingService.list(new PartFilter());

        assertThat(recording.seenOrganizationIds).containsOnly(orgId);
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        PartApplicationService noContext = new PartApplicationService(
                repository, fakeContext(null), PartMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        assertThatThrownBy(() -> noContext.list(new PartFilter()))
                .isInstanceOf(OrganizationContextNotAvailableException.class);
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

    private static final class RecordingPartRepository implements PartRepository {

        private final List<OrganizationId> seenOrganizationIds = new ArrayList<>();
        private final InMemoryPartRepository delegate = new InMemoryPartRepository();

        @Override
        public void save(Part part) {
            delegate.save(part);
        }

        @Override
        public Optional<Part> findById(PartId id, OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.findById(id, organizationId);
        }

        @Override
        public List<Part> findAll(OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.findAll(organizationId);
        }

        @Override
        public boolean existsByNumber(String number, OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.existsByNumber(number, organizationId);
        }
    }
}
