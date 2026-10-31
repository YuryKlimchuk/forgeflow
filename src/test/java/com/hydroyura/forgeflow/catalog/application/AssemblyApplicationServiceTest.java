package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyFilter;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryAssemblyRepository;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettingsProvider;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AssemblyApplicationServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final InMemoryAssemblyRepository repository = new InMemoryAssemblyRepository();
    private OrganizationSettings settings = OrganizationSettings.empty();
    private final OrganizationSettingsProvider settingsProvider = ignored -> settings;
    private final AssemblyApplicationService service = new AssemblyApplicationService(
            repository, fakeContext(orgId), settingsProvider, AssemblyMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void create_returnsViewWithSystemFields() {
        AssemblyView view = service.create(new CreateAssemblyRequest("А-1", "Узел"));

        assertThat(view.id()).isNotNull();
        assertThat(view.number()).isEqualTo("А-1");
        assertThat(view.name()).isEqualTo("Узел");
        assertThat(view.createdAt()).isEqualTo(FIXED);
        assertThat(view.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void create_rejectsDuplicateNumber() {
        service.create(new CreateAssemblyRequest("А-1", "Узел"));

        assertThatThrownBy(() -> service.create(new CreateAssemblyRequest("А-1", "Другой")))
                .isInstanceOf(DuplicateAssemblyNumberException.class);
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new CreateAssemblyRequest("А-1", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsNumberNotMatchingPattern() {
        settings = new OrganizationSettings(null, "ASM-[0-9]+", null, null, null);

        assertThatThrownBy(() -> service.create(new CreateAssemblyRequest("7", "Узел")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    void create_acceptsNumberMatchingPattern() {
        settings = new OrganizationSettings(null, "ASM-[0-9]+", null, null, null);

        AssemblyView view = service.create(new CreateAssemblyRequest("ASM-7", "Узел"));

        assertThat(view.number()).isEqualTo("ASM-7");
    }

    @Test
    void get_returnsActiveAssembly() {
        AssemblyView created = service.create(new CreateAssemblyRequest("А-1", "Узел"));

        AssemblyView view = service.get(created.id());

        assertThat(view).isEqualTo(created);
    }

    @Test
    void get_throwsWhenMissing() {
        assertThatThrownBy(() -> service.get(AssemblyId.generate()))
                .isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void get_throwsWhenDeleted() {
        AssemblyView created = service.create(new CreateAssemblyRequest("А-1", "Узел"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void get_throwsWhenForeignRecord() {
        Assembly foreign = Assembly.create(
                AssemblyId.generate(), "А-99", "Чужая", OrganizationId.generate(), FIXED, FIXED);
        repository.save(foreign);

        assertThatThrownBy(() -> service.get(foreign.getId()))
                .isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void list_returnsOnlyActiveSortedByNumber() {
        service.create(new CreateAssemblyRequest("А-2", "Вторая"));
        service.create(new CreateAssemblyRequest("А-1", "Первая"));
        AssemblyView deleted = service.create(new CreateAssemblyRequest("А-0", "Удалённая"));
        service.delete(deleted.id());

        List<AssemblyView> result = service.list(new AssemblyFilter());

        assertThat(result).extracting(AssemblyView::number).containsExactly("А-1", "А-2");
    }

    @Test
    void list_excludesForeignRecords() {
        service.create(new CreateAssemblyRequest("А-1", "Своя"));
        repository.save(Assembly.create(
                AssemblyId.generate(), "А-99", "Чужая", OrganizationId.generate(), FIXED, FIXED));

        List<AssemblyView> result = service.list(new AssemblyFilter());

        assertThat(result).extracting(AssemblyView::number).containsExactly("А-1");
    }

    @Test
    void rename_updatesName() {
        AssemblyView created = service.create(new CreateAssemblyRequest("А-1", "Узел"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_doesNotValidateNumber() {
        settings = new OrganizationSettings(null, "ASM-[0-9]+", null, null, null);
        AssemblyView created = service.create(new CreateAssemblyRequest("ASM-1", "Узел"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_throwsWhenMissing() {
        assertThatThrownBy(() -> service.rename(AssemblyId.generate(), "Имя"))
                .isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        AssemblyView created = service.create(new CreateAssemblyRequest("А-1", "Узел"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(AssemblyNotFoundException.class);
        assertThat(service.list(new AssemblyFilter())).isEmpty();
    }

    @Test
    void delete_throwsWhenAlreadyDeletedOrMissing() {
        AssemblyView created = service.create(new CreateAssemblyRequest("А-1", "Узел"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(AssemblyNotFoundException.class);
        assertThatThrownBy(() -> service.delete(AssemblyId.generate())).isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void passesOrganizationIdFromContextToRepository() {
        RecordingAssemblyRepository recording = new RecordingAssemblyRepository();
        AssemblyApplicationService recordingService = new AssemblyApplicationService(
                recording, fakeContext(orgId), settingsProvider, AssemblyMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        recordingService.create(new CreateAssemblyRequest("А-1", "Узел"));
        recordingService.list(new AssemblyFilter());

        assertThat(recording.seenOrganizationIds).containsOnly(orgId);
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        AssemblyApplicationService noContext = new AssemblyApplicationService(
                repository, fakeContext(null), settingsProvider, AssemblyMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        assertThatThrownBy(() -> noContext.list(new AssemblyFilter()))
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

    private static final class RecordingAssemblyRepository implements AssemblyRepository {

        private final List<OrganizationId> seenOrganizationIds = new ArrayList<>();
        private final InMemoryAssemblyRepository delegate = new InMemoryAssemblyRepository();

        @Override
        public void save(Assembly assembly) {
            delegate.save(assembly);
        }

        @Override
        public Optional<Assembly> findById(AssemblyId id, OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.findById(id, organizationId);
        }

        @Override
        public List<Assembly> findAll(OrganizationId organizationId) {
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
