package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.CreateStandartRequest;
import com.hydroyura.forgeflow.catalog.api.StandartFilter;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryStandartRepository;
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

class StandartApplicationServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final InMemoryStandartRepository repository = new InMemoryStandartRepository();
    private final StandartApplicationService service = new StandartApplicationService(
            repository, fakeContext(orgId), StandartMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void create_returnsViewWithSystemFields() {
        StandartView view = service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));

        assertThat(view.id()).isNotNull();
        assertThat(view.number()).isEqualTo("С-1");
        assertThat(view.name()).isEqualTo("Стандартное изделие");
        assertThat(view.createdAt()).isEqualTo(FIXED);
        assertThat(view.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void create_rejectsDuplicateNumber() {
        service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));

        assertThatThrownBy(() -> service.create(new CreateStandartRequest("С-1", "Другое")))
                .isInstanceOf(DuplicateStandartNumberException.class);
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new CreateStandartRequest("С-1", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void get_returnsActiveStandart() {
        StandartView created = service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));

        StandartView view = service.get(created.id());

        assertThat(view).isEqualTo(created);
    }

    @Test
    void get_throwsWhenMissing() {
        assertThatThrownBy(() -> service.get(StandartId.generate()))
                .isInstanceOf(StandartNotFoundException.class);
    }

    @Test
    void get_throwsWhenDeleted() {
        StandartView created = service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(StandartNotFoundException.class);
    }

    @Test
    void get_throwsWhenForeignRecord() {
        Standart foreign = Standart.create(
                StandartId.generate(), "С-99", "Чужое", OrganizationId.generate(), FIXED, FIXED);
        repository.save(foreign);

        assertThatThrownBy(() -> service.get(foreign.getId()))
                .isInstanceOf(StandartNotFoundException.class);
    }

    @Test
    void list_returnsOnlyActiveSortedByNumber() {
        service.create(new CreateStandartRequest("С-2", "Второе"));
        service.create(new CreateStandartRequest("С-1", "Первое"));
        StandartView deleted = service.create(new CreateStandartRequest("С-0", "Удалённое"));
        service.delete(deleted.id());

        List<StandartView> result = service.list(new StandartFilter());

        assertThat(result).extracting(StandartView::number).containsExactly("С-1", "С-2");
    }

    @Test
    void list_excludesForeignRecords() {
        service.create(new CreateStandartRequest("С-1", "Своё"));
        repository.save(Standart.create(
                StandartId.generate(), "С-99", "Чужое", OrganizationId.generate(), FIXED, FIXED));

        List<StandartView> result = service.list(new StandartFilter());

        assertThat(result).extracting(StandartView::number).containsExactly("С-1");
    }

    @Test
    void rename_updatesName() {
        StandartView created = service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_throwsWhenMissing() {
        assertThatThrownBy(() -> service.rename(StandartId.generate(), "Имя"))
                .isInstanceOf(StandartNotFoundException.class);
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        StandartView created = service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(StandartNotFoundException.class);
        assertThat(service.list(new StandartFilter())).isEmpty();
    }

    @Test
    void delete_throwsWhenAlreadyDeletedOrMissing() {
        StandartView created = service.create(new CreateStandartRequest("С-1", "Стандартное изделие"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(StandartNotFoundException.class);
        assertThatThrownBy(() -> service.delete(StandartId.generate())).isInstanceOf(StandartNotFoundException.class);
    }

    @Test
    void passesOrganizationIdFromContextToRepository() {
        RecordingStandartRepository recording = new RecordingStandartRepository();
        StandartApplicationService recordingService = new StandartApplicationService(
                recording, fakeContext(orgId), StandartMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        recordingService.create(new CreateStandartRequest("С-1", "Стандартное изделие"));
        recordingService.list(new StandartFilter());

        assertThat(recording.seenOrganizationIds).containsOnly(orgId);
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        StandartApplicationService noContext = new StandartApplicationService(
                repository, fakeContext(null), StandartMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        assertThatThrownBy(() -> noContext.list(new StandartFilter()))
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

    private static final class RecordingStandartRepository implements StandartRepository {

        private final List<OrganizationId> seenOrganizationIds = new ArrayList<>();
        private final InMemoryStandartRepository delegate = new InMemoryStandartRepository();

        @Override
        public void save(Standart standart) {
            delegate.save(standart);
        }

        @Override
        public Optional<Standart> findById(StandartId id, OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.findById(id, organizationId);
        }

        @Override
        public List<Standart> findAll(OrganizationId organizationId) {
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
