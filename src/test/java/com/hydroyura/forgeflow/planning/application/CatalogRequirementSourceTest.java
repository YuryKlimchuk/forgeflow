package com.hydroyura.forgeflow.planning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogRequirementSourceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final SpecificationService specificationService = mock(SpecificationService.class);
    private final AssemblyService assemblyService = mock(AssemblyService.class);
    private final PartService partService = mock(PartService.class);
    private final BuyService buyService = mock(BuyService.class);
    private final StandartService standartService = mock(StandartService.class);
    private final VzkService vzkService = mock(VzkService.class);

    private final CatalogRequirementSource source = new CatalogRequirementSource(
            specificationService, assemblyService, partService, buyService, standartService, vzkService);

    @Test
    void rowsMapsMainRowsIgnoringSubstitutes() {
        AssemblyId assembly = AssemblyId.generate();
        CatalogItemReference part = new CatalogItemReference(CatalogItemType.PART, UUID.randomUUID());
        when(specificationService.get(assembly)).thenReturn(List.of(
                new SpecificationRowView(part, 3, true)));

        List<RequirementSource.SourceRow> rows = source.rows(assembly);

        assertThat(rows).containsExactly(new RequirementSource.SourceRow(part, 3));
    }

    @Test
    void descriptorResolvesEachCatalogType() {
        UUID assemblyId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID buyId = UUID.randomUUID();
        UUID standartId = UUID.randomUUID();
        UUID vzkId = UUID.randomUUID();

        when(assemblyService.get(AssemblyId.of(assemblyId)))
                .thenReturn(new AssemblyView(AssemblyId.of(assemblyId), "СБ-0001", "Насос", NOW, NOW));
        when(partService.get(PartId.of(partId)))
                .thenReturn(new PartView(PartId.of(partId), "Д-0001", "Втулка", NOW, NOW));
        when(buyService.get(BuyId.of(buyId)))
                .thenReturn(new BuyView(BuyId.of(buyId), "П-0001", "Подшипник", NOW, NOW));
        when(standartService.get(StandartId.of(standartId)))
                .thenReturn(new StandartView(StandartId.of(standartId), "С-0001", "Болт", NOW, NOW));
        when(vzkService.get(VzkId.of(vzkId)))
                .thenReturn(new VzkView(VzkId.of(vzkId), "В-0001", "Группа ВЗК", NOW, NOW));

        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyId)))
                .contains(new RequirementSource.ItemDescriptor("СБ-0001", "Насос"));
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.PART, partId)))
                .contains(new RequirementSource.ItemDescriptor("Д-0001", "Втулка"));
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.BUY, buyId)))
                .contains(new RequirementSource.ItemDescriptor("П-0001", "Подшипник"));
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.STANDART, standartId)))
                .contains(new RequirementSource.ItemDescriptor("С-0001", "Болт"));
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.VZK, vzkId)))
                .contains(new RequirementSource.ItemDescriptor("В-0001", "Группа ВЗК"));
    }

    @Test
    void descriptorReturnsEmptyForMissingItem() {
        UUID assemblyId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID buyId = UUID.randomUUID();
        UUID standartId = UUID.randomUUID();
        UUID vzkId = UUID.randomUUID();

        when(assemblyService.get(AssemblyId.of(assemblyId))).thenThrow(new AssemblyNotFoundException(AssemblyId.of(assemblyId)));
        when(partService.get(PartId.of(partId))).thenThrow(new PartNotFoundException(PartId.of(partId)));
        when(buyService.get(BuyId.of(buyId))).thenThrow(new BuyNotFoundException(BuyId.of(buyId)));
        when(standartService.get(StandartId.of(standartId))).thenThrow(new StandartNotFoundException(StandartId.of(standartId)));
        when(vzkService.get(VzkId.of(vzkId))).thenThrow(new VzkNotFoundException(VzkId.of(vzkId)));

        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyId))).isEmpty();
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.PART, partId))).isEmpty();
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.BUY, buyId))).isEmpty();
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.STANDART, standartId))).isEmpty();
        assertThat(source.descriptor(new CatalogItemReference(CatalogItemType.VZK, vzkId))).isEmpty();
    }
}
