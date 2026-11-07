package com.hydroyura.forgeflow.technology.application;

import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.CreateEquipmentRequest;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.EquipmentService;
import com.hydroyura.forgeflow.technology.api.EquipmentView;
import com.hydroyura.forgeflow.technology.api.UpdateEquipmentRequest;
import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentNotFoundException;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link EquipmentService}: управление оборудованием.
 *
 * <p>Текущая организация берётся из {@link OrganizationContext} и передаётся в
 * репозиторий явным аргументом; репозиторий контекст не читает.</p>
 */
@Transactional
public class EquipmentApplicationService implements EquipmentService {

    private final EquipmentRepository repository;
    private final OrganizationContext organizationContext;
    private final EquipmentMapper mapper;
    private final Clock clock;

    public EquipmentApplicationService(
            EquipmentRepository repository,
            OrganizationContext organizationContext,
            EquipmentMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public EquipmentView create(CreateEquipmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateEquipmentNumberException(request.number());
        }
        Instant now = clock.instant();
        Equipment equipment = Equipment.create(
                EquipmentId.generate(),
                request.number(),
                request.name(),
                request.type(),
                organizationId,
                now,
                now);
        repository.save(equipment);
        return mapper.toView(equipment);
    }

    @Override
    public EquipmentView get(EquipmentId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Equipment equipment = repository.findById(id, organizationId)
                .orElseThrow(() -> new EquipmentNotFoundException(id));
        return mapper.toView(equipment);
    }

    @Override
    public List<EquipmentView> list() {
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void update(EquipmentId id, UpdateEquipmentRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Equipment equipment = repository.findById(id, organizationId)
                .orElseThrow(() -> new EquipmentNotFoundException(id));
        equipment.update(request.name(), request.type());
        repository.save(equipment);
    }

    @Override
    public void delete(EquipmentId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Equipment equipment = repository.findById(id, organizationId)
                .orElseThrow(() -> new EquipmentNotFoundException(id));
        equipment.markDeleted();
        repository.save(equipment);
    }
}
