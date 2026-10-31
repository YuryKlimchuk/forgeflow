package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.BuyFilter;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.api.CreateBuyRequest;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryBuyRepository;
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

class BuyApplicationServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final InMemoryBuyRepository repository = new InMemoryBuyRepository();
    private OrganizationSettings settings = OrganizationSettings.empty();
    private final OrganizationSettingsProvider settingsProvider = ignored -> settings;
    private final BuyApplicationService service = new BuyApplicationService(
            repository, fakeContext(orgId), settingsProvider, BuyMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void create_returnsViewWithSystemFields() {
        BuyView view = service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));

        assertThat(view.id()).isNotNull();
        assertThat(view.number()).isEqualTo("Б-1");
        assertThat(view.name()).isEqualTo("Покупное изделие");
        assertThat(view.createdAt()).isEqualTo(FIXED);
        assertThat(view.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void create_rejectsDuplicateNumber() {
        service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));

        assertThatThrownBy(() -> service.create(new CreateBuyRequest("Б-1", "Другое")))
                .isInstanceOf(DuplicateBuyNumberException.class);
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new CreateBuyRequest("Б-1", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsNumberNotMatchingPattern() {
        settings = new OrganizationSettings(null, null, "BUY-[0-9]+", null, null);

        assertThatThrownBy(() -> service.create(new CreateBuyRequest("7", "Покупное изделие")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    void create_acceptsNumberMatchingPattern() {
        settings = new OrganizationSettings(null, null, "BUY-[0-9]+", null, null);

        BuyView view = service.create(new CreateBuyRequest("BUY-7", "Покупное изделие"));

        assertThat(view.number()).isEqualTo("BUY-7");
    }

    @Test
    void get_returnsActiveBuy() {
        BuyView created = service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));

        BuyView view = service.get(created.id());

        assertThat(view).isEqualTo(created);
    }

    @Test
    void get_throwsWhenMissing() {
        assertThatThrownBy(() -> service.get(BuyId.generate()))
                .isInstanceOf(BuyNotFoundException.class);
    }

    @Test
    void get_throwsWhenDeleted() {
        BuyView created = service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(BuyNotFoundException.class);
    }

    @Test
    void get_throwsWhenForeignRecord() {
        Buy foreign = Buy.create(
                BuyId.generate(), "Б-99", "Чужое", OrganizationId.generate(), FIXED, FIXED);
        repository.save(foreign);

        assertThatThrownBy(() -> service.get(foreign.getId()))
                .isInstanceOf(BuyNotFoundException.class);
    }

    @Test
    void list_returnsOnlyActiveSortedByNumber() {
        service.create(new CreateBuyRequest("Б-2", "Второе"));
        service.create(new CreateBuyRequest("Б-1", "Первое"));
        BuyView deleted = service.create(new CreateBuyRequest("Б-0", "Удалённое"));
        service.delete(deleted.id());

        List<BuyView> result = service.list(new BuyFilter());

        assertThat(result).extracting(BuyView::number).containsExactly("Б-1", "Б-2");
    }

    @Test
    void list_excludesForeignRecords() {
        service.create(new CreateBuyRequest("Б-1", "Своё"));
        repository.save(Buy.create(
                BuyId.generate(), "Б-99", "Чужое", OrganizationId.generate(), FIXED, FIXED));

        List<BuyView> result = service.list(new BuyFilter());

        assertThat(result).extracting(BuyView::number).containsExactly("Б-1");
    }

    @Test
    void rename_updatesName() {
        BuyView created = service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_doesNotValidateNumber() {
        settings = new OrganizationSettings(null, null, "BUY-[0-9]+", null, null);
        BuyView created = service.create(new CreateBuyRequest("BUY-1", "Покупное изделие"));

        service.rename(created.id(), "Новое имя");

        assertThat(service.get(created.id()).name()).isEqualTo("Новое имя");
    }

    @Test
    void rename_throwsWhenMissing() {
        assertThatThrownBy(() -> service.rename(BuyId.generate(), "Имя"))
                .isInstanceOf(BuyNotFoundException.class);
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        BuyView created = service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(BuyNotFoundException.class);
        assertThat(service.list(new BuyFilter())).isEmpty();
    }

    @Test
    void delete_throwsWhenAlreadyDeletedOrMissing() {
        BuyView created = service.create(new CreateBuyRequest("Б-1", "Покупное изделие"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(BuyNotFoundException.class);
        assertThatThrownBy(() -> service.delete(BuyId.generate())).isInstanceOf(BuyNotFoundException.class);
    }

    @Test
    void passesOrganizationIdFromContextToRepository() {
        RecordingBuyRepository recording = new RecordingBuyRepository();
        BuyApplicationService recordingService = new BuyApplicationService(
                recording, fakeContext(orgId), settingsProvider, BuyMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        recordingService.create(new CreateBuyRequest("Б-1", "Покупное изделие"));
        recordingService.list(new BuyFilter());

        assertThat(recording.seenOrganizationIds).containsOnly(orgId);
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        BuyApplicationService noContext = new BuyApplicationService(
                repository, fakeContext(null), settingsProvider, BuyMapper.INSTANCE, Clock.fixed(FIXED, ZoneOffset.UTC));

        assertThatThrownBy(() -> noContext.list(new BuyFilter()))
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

    private static final class RecordingBuyRepository implements BuyRepository {

        private final List<OrganizationId> seenOrganizationIds = new ArrayList<>();
        private final InMemoryBuyRepository delegate = new InMemoryBuyRepository();

        @Override
        public void save(Buy buy) {
            delegate.save(buy);
        }

        @Override
        public Optional<Buy> findById(BuyId id, OrganizationId organizationId) {
            seenOrganizationIds.add(organizationId);
            return delegate.findById(id, organizationId);
        }

        @Override
        public List<Buy> findAll(OrganizationId organizationId) {
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
