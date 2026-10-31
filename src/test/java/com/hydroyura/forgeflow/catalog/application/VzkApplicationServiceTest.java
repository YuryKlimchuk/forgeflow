package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.CreateVzkRequest;
import com.hydroyura.forgeflow.catalog.api.VzkFilter;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.catalog.domain.vzk.VzkRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryVzkRepository;
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

class VzkApplicationServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final InMemoryVzkRepository repository = new InMemoryVzkRepository();
    private OrganizationSettings settings = OrganizationSettings.empty();
    private final OrganizationSettingsProvider settingsProvider = ignored -> settings;
    private final VzkApplicationService service = new VzkApplicationService(
            repository, fakeContext(orgId), settingsProvider, VzkMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void create_returnsViewWithSystemFields() {
        VzkView view = service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));

        assertThat(view.id()).isNotNull();
        assertThat(view.number()).isEqualTo("В-1");
        assertThat(view.name()).isEqualTo("Группа ВЗК");
        assertThat(view.createdAt()).isEqualTo(FIXED);
        assertThat(view.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void create_rejectsDuplicateNumber() {
        service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));

        assertThatThrownBy(() -> service.create(new CreateVzkRequest("В-1", "Другая")))
                .isInstanceOf(DuplicateVzkNumberException.class);
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new CreateVzkRequest("В-1", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsNumberNotMatchingPattern() {
        settings = new OrganizationSettings(null, null, null, null, "VZK-[0-9]+");

        assertThatThrownBy(() -> service.create(new CreateVzkRequest("7", "Группа ВЗК")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    void create_acceptsNumberMatchingPattern() {
        settings = new OrganizationSettings(null, null, null, null, "VZK-[0-9]+");

        VzkView view = service.create(new CreateVzkRequest("VZK-7", "Группа ВЗК"));

        assertThat(view.number()).isEqualTo("VZK-7");
    }

    @Test
    void get_returnsActiveVzk() {
        VzkView created = service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));

        VzkView view = service.get(created.id());

        assertThat(view).isEqualTo(created);
    }

    @Test
    void get_throwsWhenMissing() {
        assertThatThrownBy(() -> service.get(VzkId.generate()))
                .isInstanceOf(VzkNotFoundException.class);
    }

    @Test
    void get_throwsWhenDeleted() {
        VzkView created = service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(VzkNotFoundException.class);
    }

    @Test
    void get_throwsWhenForeignRecord() {
        Vzk foreign = Vzk.create(
                VzkId.generate(), "В-99", "Чужая", OrganizationId.generate(), FIXED, FIXED);
        repository.save(foreign);

        assertThatThrownBy(() -> service.get(foreign.getId()))
                .isInstanceOf(VzkNotFoundException.class);
    }

    @Test
    void list_returnsOnlyActiveSortedByNumber() {
        service.create(new CreateVzkRequest("В-2", "Вторая"));
        service.create(new CreateVzkRequest("В-1", "Первая"));
        VzkView deleted = service.create(new CreateVzkRequest("В-0", "Удалённая"));
        service.delete(deleted.id());

        List<VzkView> result = service.list(new VzkFilter());

        assertThat(result).extracting(VzkView::number).containsExactly("В-1", "В-2");
    }

    @Test
    void list_excludesForeignRecords() {
        service.create(new CreateVzkRequest("В-1", "Своя"));
        repository.save(Vzk.create(
                VzkId.generate(), "В-99", "Чужая", OrganizationId.generate(), FIXED, FIXED));

        List<VzkView> result = service.list(new VzkFilter());

        assertThat(result).extracting(VzkView::number).containsExactly("В-1");
    }

    @Test
    void rename_updatesName() {
        VzkView created = service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_doesNotValidateNumber() {
        settings = new OrganizationSettings(null, null, null, null, "VZK-[0-9]+");
        VzkView created = service.create(new CreateVzkRequest("VZK-1", "Группа ВЗК"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_throwsWhenMissing() {
        assertThatThrownBy(() -> service.rename(VzkId.generate(), "Имя"))
                .isInstanceOf(VzkNotFoundException.class);
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        VzkView created = service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(VzkNotFoundException.class);
        assertThat(service.list(new VzkFilter())).isEmpty();
    }

    @Test
    void delete_throwsWhenAlreadyDeletedOrMissing() {
        VzkView created = service.create(new CreateVzkRequest("В-1", "Группа ВЗК"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(VzkNotFoundException.class);
        assertThatThrownBy(() -> service.delete(VzkId.generate())).isInstanceOf(VzkNotFoundException.class);
    }

    @Test
    void passesOrganizationIdFromContextToRepository() {
        RecordingVzkRepository recording = new RecordingVzkRepository();
        VzkApplicationService recordingService = new VzkApplicationService(
                recording, fakeContext(orgId), settingsProvider, VzkMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        recordingService.create(new CreateVzkRequest("В-1", "Группа ВЗК"));
        recordingService.list(new VzkFilter());

        assertThat(recording.seenOrganizationIds).containsOnly(orgId);
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        VzkApplicationService noContext = new VzkApplicationService(
                repository, fakeContext(null), settingsProvider, VzkMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        assertThatThrownBy(() -> noContext.list(new VzkFilter()))
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

    private static final class RecordingVzkRepository implements VzkRepository {

        private final List<OrganizationId> seenOrganizationIds = new ArrayList<>();
        private final InMemoryVzkRepository delegate = new InMemoryVzkRepository();

        @Override
        public void save(Vzk vzk) {
            delegate.save(vzk);
        }

        @Override
        public Optional<Vzk> findById(VzkId id, OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.findById(id, organizationId);
        }

        @Override
        public List<Vzk> findAll(OrganizationId organizationId) {
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
