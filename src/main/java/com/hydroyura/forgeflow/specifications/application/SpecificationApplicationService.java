package com.hydroyura.forgeflow.specifications.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.specifications.api.AddRowRequest;
import com.hydroyura.forgeflow.specifications.api.AddSubstituteRequest;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.api.ChangeQuantityRequest;
import com.hydroyura.forgeflow.specifications.api.CopySpecificationRequest;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import com.hydroyura.forgeflow.specifications.api.RemoveRowRequest;
import com.hydroyura.forgeflow.specifications.api.RemoveSubstituteRequest;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import com.hydroyura.forgeflow.specifications.domain.BomCycleException;
import com.hydroyura.forgeflow.specifications.domain.Quantity;
import com.hydroyura.forgeflow.specifications.domain.RowNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRow;
import com.hydroyura.forgeflow.specifications.domain.SubstituteNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.Substitution;
import com.hydroyura.forgeflow.specifications.domain.SubstitutionRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/**
 * Реализация {@link SpecificationService}: CRUD прямых строк спецификации,
 * копирование и плоское разворачивание. Текущая организация берётся из
 * {@link OrganizationContext} и передаётся в репозиторий явным аргументом.
 */
@Transactional
public class SpecificationApplicationService implements SpecificationService {

    private final SpecificationRepository repository;
    private final SubstitutionRepository substitutionRepository;
    private final OrganizationContext organizationContext;
    private final AssemblyService assemblyService;
    private final PartService partService;
    private final BuyService buyService;
    private final StandartService standartService;
    private final VzkService vzkService;
    private final SpecificationMapper mapper;

    public SpecificationApplicationService(
            SpecificationRepository repository,
            SubstitutionRepository substitutionRepository,
            OrganizationContext organizationContext,
            AssemblyService assemblyService,
            PartService partService,
            BuyService buyService,
            StandartService standartService,
            VzkService vzkService,
            SpecificationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.substitutionRepository = Objects.requireNonNull(substitutionRepository, "substitutionRepository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService must not be null");
        this.partService = Objects.requireNonNull(partService, "partService must not be null");
        this.buyService = Objects.requireNonNull(buyService, "buyService must not be null");
        this.standartService = Objects.requireNonNull(standartService, "standartService must not be null");
        this.vzkService = Objects.requireNonNull(vzkService, "vzkService must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<SpecificationRowView> get(AssemblyId assemblyId) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        return repository.findById(organizationId, assemblyId)
                .map(Specification::rows)
                .orElseGet(List::of)
                .stream()
                .map(row -> mapper.toView(row, hasSubstitutes(organizationId, assemblyId, row)))
                .toList();
    }

    @Override
    public SpecificationRowView addRow(AssemblyId assemblyId, AddRowRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        Quantity quantity = new Quantity(request.quantity());
        Specification specification = repository.findById(organizationId, assemblyId)
                .orElseGet(() -> Specification.create(organizationId, assemblyId));
        specification.addRow(request.item(), quantity);
        repository.save(specification);
        return new SpecificationRowView(request.item(), quantity.value(), false);
    }

    @Override
    public void changeQuantity(AssemblyId assemblyId, ChangeQuantityRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        Quantity quantity = new Quantity(request.quantity());
        Specification specification = repository.findById(organizationId, assemblyId)
                .orElseThrow(() -> new RowNotFoundException(request.item()));
        specification.changeQuantity(request.item(), quantity);
        repository.save(specification);
    }

    @Override
    public void removeRow(AssemblyId assemblyId, RemoveRowRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        Specification specification = repository.findById(organizationId, assemblyId)
                .orElseThrow(() -> new RowNotFoundException(request.item()));
        specification.removeRow(request.item());
        repository.save(specification);
        substitutionRepository.delete(organizationId, assemblyId, request.item());
    }

    @Override
    public AssemblyView copy(AssemblyId sourceAssemblyId, CopySpecificationRequest request) {
        Objects.requireNonNull(sourceAssemblyId, "sourceAssemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(sourceAssemblyId);
        List<SpecificationRow> sourceRows = repository.findById(organizationId, sourceAssemblyId)
                .map(Specification::rows)
                .orElseGet(List::of);
        AssemblyView newAssembly = assemblyService.create(new CreateAssemblyRequest(request.number(), request.name()));
        Specification newSpecification = Specification.create(organizationId, newAssembly.id());
        for (SpecificationRow row : sourceRows) {
            newSpecification.addRow(row.item(), row.quantity());
        }
        repository.save(newSpecification);
        for (SpecificationRow row : sourceRows) {
            copySubstitutes(organizationId, sourceAssemblyId, newAssembly.id(), row);
        }
        return newAssembly;
    }

    @Override
    public List<FlatBomItem> flatten(AssemblyId assemblyId) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);

        Map<CatalogItemReference, Integer> leafCounts = new LinkedHashMap<>();
        traverse(organizationId, assemblyId, 1, new HashSet<>(), leafCounts);

        List<FlatBomItem> result = new ArrayList<>();
        for (Map.Entry<CatalogItemReference, Integer> entry : leafCounts.entrySet()) {
            CatalogItemReference reference = entry.getKey();
            result.add(toFlatItem(reference, entry.getValue()));
        }
        result.sort(Comparator.comparing(FlatBomItem::number));
        return result;
    }

    private void traverse(
            OrganizationId organizationId,
            AssemblyId assemblyId,
            int multiplier,
            Set<AssemblyId> path,
            Map<CatalogItemReference, Integer> leafCounts) {
        if (!path.add(assemblyId)) {
            throw new BomCycleException(assemblyId);
        }
        try {
            Specification specification = repository.findById(organizationId, assemblyId).orElse(null);
            if (specification == null) {
                return;
            }
            for (SpecificationRow row : specification.rows()) {
                int quantity = Math.multiplyExact(multiplier, row.quantity().value());
                if (row.item().type() == CatalogItemType.ASSEMBLY) {
                    traverse(organizationId, AssemblyId.of(row.item().id()), quantity, path, leafCounts);
                } else {
                    leafCounts.merge(row.item(), quantity, Math::addExact);
                }
            }
        } finally {
            path.remove(assemblyId);
        }
    }

    private FlatBomItem toFlatItem(CatalogItemReference reference, int quantity) {
        return switch (reference.type()) {
            case PART -> {
                PartView view = partService.get(PartId.of(reference.id()));
                yield new FlatBomItem(view.number(), view.name(), quantity);
            }
            case BUY -> {
                BuyView view = buyService.get(BuyId.of(reference.id()));
                yield new FlatBomItem(view.number(), view.name(), quantity);
            }
            case STANDART -> {
                StandartView view = standartService.get(StandartId.of(reference.id()));
                yield new FlatBomItem(view.number(), view.name(), quantity);
            }
            case VZK -> {
                VzkView view = vzkService.get(VzkId.of(reference.id()));
                yield new FlatBomItem(view.number(), view.name(), quantity);
            }
            case ASSEMBLY -> throw new IllegalStateException("Assembly must not be a leaf: " + reference.id());
        };
    }

    /**
     * Проверяет, что сборка существует, активна и принадлежит текущей
     * организации. Для чужой/удалённой/отсутствующей сборки каталог бросает
     * исключение «не найдено».
     */
    private void requireAssembly(AssemblyId assemblyId) {
        assemblyService.get(assemblyId);
    }

    @Override
    public List<CatalogItemReference> listSubstitutes(AssemblyId assemblyId, CatalogItemReference item) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(item, "item must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        requireRow(organizationId, assemblyId, item);
        return substitutionRepository.findById(organizationId, assemblyId, item)
                .map(Substitution::substitutes)
                .orElseGet(List::of);
    }

    @Override
    public CatalogItemReference addSubstitute(AssemblyId assemblyId, AddSubstituteRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        requireRow(organizationId, assemblyId, request.item());
        Substitution substitution = substitutionRepository
                .findById(organizationId, assemblyId, request.item())
                .orElseGet(() -> Substitution.create(organizationId, assemblyId, request.item()));
        substitution.addSubstitute(request.substituteRef());
        substitutionRepository.save(substitution);
        return request.substituteRef();
    }

    @Override
    public void removeSubstitute(AssemblyId assemblyId, RemoveSubstituteRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        requireRow(organizationId, assemblyId, request.item());
        Substitution substitution = substitutionRepository
                .findById(organizationId, assemblyId, request.item())
                .orElseThrow(() -> new SubstituteNotFoundException(request.substituteRef()));
        substitution.removeSubstitute(request.substituteRef());
        substitutionRepository.save(substitution);
    }

    private boolean hasSubstitutes(OrganizationId organizationId, AssemblyId assemblyId, SpecificationRow row) {
        return substitutionRepository.findById(organizationId, assemblyId, row.item())
                .map(substitution -> !substitution.substitutes().isEmpty())
                .orElse(false);
    }

    private void requireRow(OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        Specification specification = repository.findById(organizationId, assemblyId).orElse(null);
        boolean found = specification != null
                && specification.rows().stream().anyMatch(row -> row.item().equals(item));
        if (!found) {
            throw new RowNotFoundException(item);
        }
    }

    private void copySubstitutes(
            OrganizationId organizationId,
            AssemblyId sourceAssemblyId,
            AssemblyId targetAssemblyId,
            SpecificationRow row) {
        substitutionRepository.findById(organizationId, sourceAssemblyId, row.item()).ifPresent(source -> {
            Substitution target = Substitution.create(organizationId, targetAssemblyId, row.item());
            for (CatalogItemReference substitute : source.substitutes()) {
                target.addSubstitute(substitute);
            }
            substitutionRepository.save(target);
        });
    }
}
