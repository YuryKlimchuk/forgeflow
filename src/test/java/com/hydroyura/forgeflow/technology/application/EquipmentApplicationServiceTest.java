package com.hydroyura.forgeflow.technology.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.CreateEquipmentRequest;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.EquipmentView;
import com.hydroyura.forgeflow.technology.api.UpdateEquipmentRequest;
import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentNotFoundException;
import com.hydroyura.forgeflow.technology.infrastructure.InMemoryEquipmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EquipmentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryEquipmentRepository repository = new InMemoryEquipmentRepository();
    private final EquipmentApplicationService service = serviceFor(orgA);

    private static CreateEquipmentRequest request(String number, String name) {
        return new CreateEquipmentRequest(number, name, "Токарный");
    }

    @Test
    void create_returnsViewWithSystemFieldsAndIsVisibleInReads() {
        EquipmentView created = service.create(request("С-001", "Станок токарный"));

        assertThat(created.id()).isNotNull();
        assertThat(created.number()).isEqualTo("С-001");
        assertThat(created.name()).isEqualTo("Станок токарный");
        assertThat(created.type()).isEqualTo("Токарный");
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(service.get(created.id())).isEqualTo(created);
        assertThat(service.list()).containsExactly(created);
    }

    @Test
    void create_duplicateNumberInSameOrganization_rejects() {
        service.create(request("С-001", "Станок токарный"));

        assertThatThrownBy(() -> service.create(request("С-001", "Другой станок")))
                .isInstanceOf(DuplicateEquipmentNumberException.class);
        assertThat(repository.findAll(orgA)).hasSize(1);
    }

    @Test
    void create_sameNumberInDifferentOrganizations_allowed() {
        service.create(request("С-001", "Станок токарный"));
        EquipmentApplicationService otherOrgService = serviceFor(orgB);

        EquipmentView other = otherOrgService.create(request("С-001", "Другой станок"));

        assertThat(service.list()).extracting(EquipmentView::number).containsExactly("С-001");
        assertThat(otherOrgService.list()).extracting(EquipmentView::number).containsExactly("С-001");
        assertThat(otherOrgService.get(other.id())).isEqualTo(other);
    }

    @Test
    void create_blankField_rejects() {
        assertThatThrownBy(() -> service.create(request("   ", "Станок токарный")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        assertThatThrownBy(() -> service.create(request("С-001", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> service.create(new CreateEquipmentRequest("С-001", "Станок токарный", "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void create_usesOrganizationFromContext() {
        EquipmentView created = service.create(request("С-001", "Станок токарный"));

        assertThat(repository.findById(created.id(), orgA)).isPresent();
        assertThat(repository.findById(created.id(), orgB)).isEmpty();
    }

    @Test
    void get_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.get(EquipmentId.generate()))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void get_deleted_throwsNotFound() {
        EquipmentView created = service.create(request("С-001", "Станок токарный"));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void get_foreignEquipment_throwsNotFound() {
        EquipmentApplicationService otherOrgService = serviceFor(orgB);
        EquipmentView foreign = otherOrgService.create(request("С-001", "Чужой станок"));

        assertThatThrownBy(() -> service.get(foreign.id()))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void list_returnsSortedByNumber() {
        service.create(request("2", "Второй"));
        service.create(request("1", "Первый"));

        assertThat(service.list()).extracting(EquipmentView::number).containsExactly("1", "2");
    }

    @Test
    void list_excludesDeletedAndForeign() {
        service.create(request("1", "Свой"));
        serviceFor(orgB).create(request("3", "Чужой"));
        EquipmentView deleted = service.create(request("0", "Удалённый"));
        service.delete(deleted.id());

        assertThat(service.list()).extracting(EquipmentView::number).containsExactly("1");
    }

    @Test
    void update_changesMutableFieldsAndKeepsNumber() {
        EquipmentView created = service.create(request("С-001", "Станок токарный"));

        service.update(created.id(), new UpdateEquipmentRequest("Новый станок", "Фрезерный"));

        EquipmentView updated = service.get(created.id());
        assertThat(updated.name()).isEqualTo("Новый станок");
        assertThat(updated.type()).isEqualTo("Фрезерный");
        assertThat(updated.number()).isEqualTo("С-001");
        assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void update_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.update(EquipmentId.generate(),
                new UpdateEquipmentRequest("Имя", "Токарный")))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void update_invalidValues_rejectsAndKeepsState() {
        EquipmentView created = service.create(request("С-001", "Станок токарный"));

        assertThatThrownBy(() -> service.update(created.id(),
                new UpdateEquipmentRequest("   ", "Токарный")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.get(created.id()).name()).isEqualTo("Станок токарный");
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        EquipmentView created = service.create(request("С-001", "Станок токарный"));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(EquipmentNotFoundException.class);
        assertThat(service.list()).isEmpty();
    }

    @Test
    void delete_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.delete(EquipmentId.generate()))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void create_sameNumberAfterSoftDelete_succeeds() {
        EquipmentView first = service.create(request("С-001", "Станок токарный"));
        service.delete(first.id());

        EquipmentView second = service.create(request("С-001", "Станок токарный новый"));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(service.list()).extracting(EquipmentView::number).containsExactly("С-001");
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        EquipmentApplicationService noContext = serviceFor(null);

        assertThatThrownBy(() -> noContext.list())
                .isInstanceOf(OrganizationContextNotAvailableException.class);
    }

    private EquipmentApplicationService serviceFor(OrganizationId organizationId) {
        return new EquipmentApplicationService(
                repository,
                fakeContext(organizationId),
                new EquipmentMapper(),
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
