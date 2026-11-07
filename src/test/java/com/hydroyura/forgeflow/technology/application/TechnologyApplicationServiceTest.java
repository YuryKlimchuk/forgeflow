package com.hydroyura.forgeflow.technology.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.application.PartNotFoundException;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.AddOperationRequest;
import com.hydroyura.forgeflow.technology.api.CreateTechnologyProcessRequest;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.OperationView;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.domain.DuplicateOperationException;
import com.hydroyura.forgeflow.technology.domain.DuplicateTechnologyProcessNumberException;
import com.hydroyura.forgeflow.technology.domain.InvalidDefaultProcessStateException;
import com.hydroyura.forgeflow.technology.domain.OperationNotFoundException;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessNotFoundException;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessService;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentNotFoundException;
import com.hydroyura.forgeflow.technology.infrastructure.InMemoryEquipmentRepository;
import com.hydroyura.forgeflow.technology.infrastructure.InMemoryTechnologyProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TechnologyApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final PartId partA = PartId.generate();
    private final PartId partB = PartId.generate();
    private final InMemoryTechnologyProcessRepository repository = new InMemoryTechnologyProcessRepository();
    private final InMemoryEquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
    private final TechnologyProcessService domainService = new TechnologyProcessService(repository);
    private final PartService partService = mock(PartService.class);
    private final TechnologyApplicationService service = serviceFor(orgA);

    @BeforeEach
    void setUp() {
        when(partService.get(partA)).thenReturn(new PartView(partA, "Д-1", "Деталь A", NOW, NOW));
        when(partService.get(partB)).thenReturn(new PartView(partB, "Д-2", "Деталь B", NOW, NOW));
    }

    @Test
    void create_returnsViewWithSystemFieldsAndIsVisibleInReads() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThat(created.id()).isNotNull();
        assertThat(created.number()).isEqualTo("TP-1");
        assertThat(created.name()).isEqualTo("Фрезеровка");
        assertThat(created.partId()).isEqualTo(partA);
        assertThat(created.defaultProcess()).isFalse();
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(service.get(created.id())).isEqualTo(created);
        assertThat(service.list(null)).containsExactly(created);
    }

    @Test
    void create_blankNumber_rejectsBeforePartCheck() {
        assertThatThrownBy(() -> service.create(new CreateTechnologyProcessRequest("   ", "X", partA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
        verify(partService, never()).get(any(PartId.class));
    }

    @Test
    void create_blankName_rejects() {
        assertThatThrownBy(() -> service.create(new CreateTechnologyProcessRequest("TP-1", "", partA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_nullPartId_rejectsBeforePartCheck() {
        assertThatThrownBy(() -> service.create(new CreateTechnologyProcessRequest("TP-1", "X", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("partId");
        verify(partService, never()).get(any(PartId.class));
    }

    @Test
    void create_duplicateNumberInSameOrganization_rejects() {
        service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.create(new CreateTechnologyProcessRequest("TP-1", "Токарная", partB)))
                .isInstanceOf(DuplicateTechnologyProcessNumberException.class);
        assertThat(repository.findAll(orgA)).hasSize(1);
    }

    @Test
    void create_sameNumberInDifferentOrganizations_allowed() {
        service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        TechnologyApplicationService otherOrgService = serviceFor(orgB);

        TechnologyProcessView other = otherOrgService.create(
                new CreateTechnologyProcessRequest("TP-1", "Токарная", partA));

        assertThat(service.list(null)).extracting(TechnologyProcessView::number).containsExactly("TP-1");
        assertThat(otherOrgService.list(null)).extracting(TechnologyProcessView::number).containsExactly("TP-1");
        assertThat(otherOrgService.get(other.id())).isEqualTo(other);
    }

    @Test
    void create_foreignPart_rejects() {
        PartId foreign = PartId.generate();
        when(partService.get(foreign)).thenThrow(new PartNotFoundException(foreign));

        assertThatThrownBy(() -> service.create(new CreateTechnologyProcessRequest("TP-1", "X", foreign)))
                .isInstanceOf(PartNotFoundException.class);
        assertThat(repository.findAll(orgA)).isEmpty();
    }

    @Test
    void create_secondNonDefaultProcessWithoutDefault_rejects() {
        service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.create(new CreateTechnologyProcessRequest("TP-2", "Токарная", partA)))
                .isInstanceOf(InvalidDefaultProcessStateException.class);
    }

    @Test
    void create_afterMarkingDefault_keepsInvariant() {
        TechnologyProcessView first = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.markAsDefault(first.id());

        TechnologyProcessView second = service.create(new CreateTechnologyProcessRequest("TP-2", "Токарная", partA));

        assertThat(service.get(first.id()).defaultProcess()).isTrue();
        assertThat(second.defaultProcess()).isFalse();
    }

    @Test
    void get_missing_throwsNotFound() {
        assertThatThrownBy(() -> service.get(TechnologyProcessId.generate()))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void get_deleted_throwsNotFound() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void get_foreignProcess_throwsNotFound() {
        TechnologyApplicationService otherOrgService = serviceFor(orgB);
        TechnologyProcessView foreign = otherOrgService.create(
                new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.get(foreign.id()))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void list_returnsSortedByNumber() {
        service.create(new CreateTechnologyProcessRequest("TP-2", "Второй", partA));
        service.create(new CreateTechnologyProcessRequest("TP-1", "Первый", partB));

        assertThat(service.list(null))
                .extracting(TechnologyProcessView::number)
                .containsExactly("TP-1", "TP-2");
    }

    @Test
    void list_excludesDeletedAndForeign() {
        service.create(new CreateTechnologyProcessRequest("TP-1", "Свой", partA));
        serviceFor(orgB).create(new CreateTechnologyProcessRequest("TP-3", "Чужой", partA));
        TechnologyProcessView deleted = service.create(new CreateTechnologyProcessRequest("TP-0", "Удалённый", partB));
        service.delete(deleted.id());

        assertThat(service.list(null))
                .extracting(TechnologyProcessView::number)
                .containsExactly("TP-1");
    }

    @Test
    void list_withPartId_returnsOnlyProcessesOfPart() {
        service.create(new CreateTechnologyProcessRequest("TP-1", "Первая деталь", partA));
        service.create(new CreateTechnologyProcessRequest("TP-2", "Вторая деталь", partB));

        assertThat(service.list(partA))
                .extracting(TechnologyProcessView::number)
                .containsExactly("TP-1");
    }

    @Test
    void rename_updatesNameAndUpdatedAt() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        service.rename(created.id(), "Новое имя");

        TechnologyProcessView renamed = service.get(created.id());
        assertThat(renamed.name()).isEqualTo("Новое имя");
        assertThat(renamed.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void rename_blankName_rejectsAndKeepsName() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.rename(created.id(), "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.get(created.id()).name()).isEqualTo("Фрезеровка");
    }

    @Test
    void delete_softDeletesAndHidesFromReads() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
        assertThat(service.list(null)).isEmpty();
    }

    @Test
    void delete_singleDefaultProcess_allowed() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.markAsDefault(created.id());

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void addOperation_addsAndOrdersByNumber() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        service.addOperation(created.id(), new AddOperationRequest(20, "А"));
        service.addOperation(created.id(), new AddOperationRequest(10, "Б"));

        assertThat(service.get(created.id()).operations())
                .extracting(OperationView::number)
                .containsExactly(10, 20);
    }

    @Test
    void addOperation_duplicateNumber_rejects() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.addOperation(created.id(), new AddOperationRequest(10, "А"));

        assertThatThrownBy(() -> service.addOperation(created.id(), new AddOperationRequest(10, "Б")))
                .isInstanceOf(DuplicateOperationException.class);
    }

    @Test
    void addOperation_invalidOperation_rejects() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.addOperation(created.id(), new AddOperationRequest(0, "А")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addOperation(created.id(), new AddOperationRequest(1, " ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addOperation_withEquipmentId_addsOperationAndReturnsReference() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        EquipmentId equipmentId = EquipmentId.generate();
        equipmentRepository.save(Equipment.create(
                equipmentId, "С-001", "Станок", "Токарный", orgA, NOW, NOW));

        OperationView added = service.addOperation(created.id(), new AddOperationRequest(10, "А", equipmentId));

        assertThat(added.equipmentId()).isEqualTo(equipmentId);
        assertThat(service.get(created.id()).operations())
                .extracting(OperationView::equipmentId)
                .containsExactly(equipmentId);
    }

    @Test
    void addOperation_withoutEquipmentId_addsOperationWithoutReference() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        OperationView added = service.addOperation(created.id(), new AddOperationRequest(10, "А"));

        assertThat(added.equipmentId()).isNull();
        assertThat(service.get(created.id()).operations())
                .extracting(OperationView::equipmentId)
                .containsExactly((EquipmentId) null);
    }

    @Test
    void addOperation_foreignEquipment_rejectsAndKeepsProcessUnchanged() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        EquipmentId foreignEquipmentId = EquipmentId.generate();
        equipmentRepository.save(Equipment.create(
                foreignEquipmentId, "С-001", "Чужой станок", "Токарный", orgB, NOW, NOW));
        TechnologyProcessView before = service.get(created.id());

        assertThatThrownBy(() -> service.addOperation(
                created.id(), new AddOperationRequest(10, "А", foreignEquipmentId)))
                .isInstanceOf(EquipmentNotFoundException.class);
        assertThat(service.get(created.id())).isEqualTo(before);
    }

    @Test
    void addOperation_missingEquipment_rejectsAndKeepsProcessUnchanged() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        TechnologyProcessView before = service.get(created.id());

        assertThatThrownBy(() -> service.addOperation(
                created.id(), new AddOperationRequest(10, "А", EquipmentId.generate())))
                .isInstanceOf(EquipmentNotFoundException.class);
        assertThat(service.get(created.id())).isEqualTo(before);
    }

    @Test
    void addOperation_processMissing_rejectsBeforeEquipmentCheck() {
        assertThatThrownBy(() -> service.addOperation(
                TechnologyProcessId.generate(), new AddOperationRequest(10, "А", EquipmentId.generate())))
                .isInstanceOf(TechnologyProcessNotFoundException.class);
    }

    @Test
    void addOperation_equipmentCheckRunsBeforeOperationValidation() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.addOperation(
                created.id(), new AddOperationRequest(0, "А", EquipmentId.generate())))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void removeOperation_removesByNumber() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.addOperation(created.id(), new AddOperationRequest(10, "А"));

        service.removeOperation(created.id(), 10);

        assertThat(service.get(created.id()).operations()).isEmpty();
    }

    @Test
    void removeOperation_missingNumber_rejects() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        assertThatThrownBy(() -> service.removeOperation(created.id(), 99))
                .isInstanceOf(OperationNotFoundException.class);
    }

    @Test
    void markAsDefault_switchesDefaultWithoutTwoDefaults() {
        TechnologyProcessView first = service.create(new CreateTechnologyProcessRequest("TP-1", "Первый", partA));
        service.markAsDefault(first.id());
        TechnologyProcessView second = service.create(new CreateTechnologyProcessRequest("TP-2", "Второй", partA));

        service.markAsDefault(second.id());

        assertThat(service.get(first.id()).defaultProcess()).isFalse();
        assertThat(service.get(second.id()).defaultProcess()).isTrue();
    }

    @Test
    void markAsDefault_alreadyDefault_isNoOp() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.markAsDefault(created.id());

        service.markAsDefault(created.id());

        assertThat(service.get(created.id()).defaultProcess()).isTrue();
    }

    @Test
    void unmarkDefault_valid_clearsDefault() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));
        service.markAsDefault(created.id());

        service.unmarkDefault(created.id());

        assertThat(service.get(created.id()).defaultProcess()).isFalse();
    }

    @Test
    void unmarkDefault_soleDefaultWithOtherActiveProcess_rejects() {
        TechnologyProcessView first = service.create(new CreateTechnologyProcessRequest("TP-1", "Первый", partA));
        service.markAsDefault(first.id());
        service.create(new CreateTechnologyProcessRequest("TP-2", "Второй", partA));

        assertThatThrownBy(() -> service.unmarkDefault(first.id()))
                .isInstanceOf(InvalidDefaultProcessStateException.class);
        assertThat(service.get(first.id()).defaultProcess()).isTrue();
    }

    @Test
    void unmarkDefault_notDefault_isNoOp() {
        TechnologyProcessView created = service.create(new CreateTechnologyProcessRequest("TP-1", "Фрезеровка", partA));

        service.unmarkDefault(created.id());

        assertThat(service.get(created.id()).defaultProcess()).isFalse();
    }

    @Test
    void throwsWhenNoOrganizationContext() {
        TechnologyApplicationService noContext = serviceFor(null);

        assertThatThrownBy(() -> noContext.list(null))
                .isInstanceOf(OrganizationContextNotAvailableException.class);
    }

    private TechnologyApplicationService serviceFor(OrganizationId organizationId) {
        return new TechnologyApplicationService(
                repository,
                domainService,
                fakeContext(organizationId),
                partService,
                equipmentRepository,
                new TechnologyMapper(),
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
