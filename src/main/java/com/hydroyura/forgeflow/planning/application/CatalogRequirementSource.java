package com.hydroyura.forgeflow.planning.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.catalog.application.AssemblyNotFoundException;
import com.hydroyura.forgeflow.catalog.application.BuyNotFoundException;
import com.hydroyura.forgeflow.catalog.application.PartNotFoundException;
import com.hydroyura.forgeflow.catalog.application.StandartNotFoundException;
import com.hydroyura.forgeflow.catalog.application.VzkNotFoundException;
import com.hydroyura.forgeflow.planning.domain.RequirementSource;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Адаптер {@link RequirementSource} поверх публичных API модулей
 * {@code specifications} и {@code catalog}.
 */
public class CatalogRequirementSource implements RequirementSource {

    private final SpecificationService specificationService;
    private final AssemblyService assemblyService;
    private final PartService partService;
    private final BuyService buyService;
    private final StandartService standartService;
    private final VzkService vzkService;

    public CatalogRequirementSource(
            SpecificationService specificationService,
            AssemblyService assemblyService,
            PartService partService,
            BuyService buyService,
            StandartService standartService,
            VzkService vzkService) {
        this.specificationService = Objects.requireNonNull(specificationService, "specificationService must not be null");
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService must not be null");
        this.partService = Objects.requireNonNull(partService, "partService must not be null");
        this.buyService = Objects.requireNonNull(buyService, "buyService must not be null");
        this.standartService = Objects.requireNonNull(standartService, "standartService must not be null");
        this.vzkService = Objects.requireNonNull(vzkService, "vzkService must not be null");
    }

    @Override
    public List<SourceRow> rows(AssemblyId assemblyId) {
        return specificationService.get(assemblyId).stream()
                .map(row -> new SourceRow(row.item(), row.quantity()))
                .toList();
    }

    @Override
    public Optional<ItemDescriptor> descriptor(CatalogItemReference item) {
        try {
            return Optional.of(switch (item.type()) {
                case ASSEMBLY -> {
                    AssemblyView view = assemblyService.get(AssemblyId.of(item.id()));
                    yield new ItemDescriptor(view.number(), view.name());
                }
                case PART -> {
                    PartView view = partService.get(PartId.of(item.id()));
                    yield new ItemDescriptor(view.number(), view.name());
                }
                case BUY -> {
                    BuyView view = buyService.get(BuyId.of(item.id()));
                    yield new ItemDescriptor(view.number(), view.name());
                }
                case STANDART -> {
                    StandartView view = standartService.get(StandartId.of(item.id()));
                    yield new ItemDescriptor(view.number(), view.name());
                }
                case VZK -> {
                    VzkView view = vzkService.get(VzkId.of(item.id()));
                    yield new ItemDescriptor(view.number(), view.name());
                }
            });
        } catch (AssemblyNotFoundException
                 | PartNotFoundException
                 | BuyNotFoundException
                 | StandartNotFoundException
                 | VzkNotFoundException e) {
            return Optional.empty();
        }
    }
}
