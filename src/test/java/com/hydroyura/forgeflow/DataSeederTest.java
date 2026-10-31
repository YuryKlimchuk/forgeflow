package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryAssemblyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryBuyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryPartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryStandartRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryVzkRepository;
import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.infrastructure.InMemorySpecificationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class DataSeederTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemoryPartRepository parts = new InMemoryPartRepository();
    private final InMemoryAssemblyRepository assemblies = new InMemoryAssemblyRepository();
    private final InMemoryBuyRepository buys = new InMemoryBuyRepository();
    private final InMemoryStandartRepository standarts = new InMemoryStandartRepository();
    private final InMemoryVzkRepository vzks = new InMemoryVzkRepository();
    private final InMemorySpecificationRepository specifications = new InMemorySpecificationRepository();
    private final OrganizationService organizationService = mock(OrganizationService.class);
    private final OrganizationId orgId = OrganizationId.generate();

    private DataSeeder seeder(boolean enabled, Path catalogFile, Path specificationsFile) {
        return new DataSeeder(
                parts, assemblies, buys, standarts, vzks, specifications,
                organizationService, orgId,
                new SeedProperties(enabled, catalogFile.toString(), specificationsFile.toString()),
                objectMapper);
    }

    @Test
    void loadsAllFiveTypesSettingsAndSpecifications() throws Exception {
        Path catalog = tempDir.resolve("catalog.json");
        Files.writeString(catalog, """
                [
                  {"number":"СБ-0001","name":"Насос","type":"ASSEMBLY"},
                  {"number":"Д-0001","name":"Вал","type":"PART"},
                  {"number":"ПОК-0001","name":"Подшипник","type":"BUY"},
                  {"number":"СТ-0001","name":"Болт","type":"STANDART"},
                  {"number":"ВЗК-0001","name":"Контроль","type":"VZK"}
                ]
                """);
        Path specs = tempDir.resolve("specifications.json");
        Files.writeString(specs, """
                [
                  {"assemblyNumber":"СБ-0001","rows":[{"type":"PART","number":"Д-0001","quantity":2}]}
                ]
                """);

        seeder(true, catalog, specs).run();

        assertThat(assemblies.findAll(orgId)).hasSize(1);
        assertThat(parts.findAll(orgId)).hasSize(1);
        assertThat(buys.findAll(orgId)).hasSize(1);
        assertThat(standarts.findAll(orgId)).hasSize(1);
        assertThat(vzks.findAll(orgId)).hasSize(1);

        var specification = specifications.findById(orgId, assemblies.findAll(orgId).get(0).getId()).orElseThrow();
        assertThat(specification.rows()).hasSize(1);
        assertThat(specification.rows().get(0).quantity().value()).isEqualTo(2);

        ArgumentCaptor<OrganizationSettings> captor = ArgumentCaptor.forClass(OrganizationSettings.class);
        verify(organizationService).updateSettings(eq(orgId), captor.capture());
        OrganizationSettings settings = captor.getValue();
        assertThat(settings.partNumberPattern()).isEqualTo("Д-\\d{4}");
        assertThat(settings.assemblyNumberPattern()).isEqualTo("СБ-\\d{4}");
        assertThat(settings.buyNumberPattern()).isEqualTo("ПОК-\\d{4}");
        assertThat(settings.standartNumberPattern()).isEqualTo("СТ-\\d{4}");
        assertThat(settings.vzkNumberPattern()).isEqualTo("ВЗК-\\d{4}");
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        seeder(false, tempDir.resolve("catalog.json"), tempDir.resolve("specifications.json")).run();

        assertThat(assemblies.findAll(orgId)).isEmpty();
        assertThat(parts.findAll(orgId)).isEmpty();
        verify(organizationService, never()).updateSettings(any(), any());
    }

    @Test
    void skipsWhenCatalogMissing() throws Exception {
        seeder(true, tempDir.resolve("nope.json"), tempDir.resolve("specifications.json")).run();

        assertThat(assemblies.findAll(orgId)).isEmpty();
        assertThat(parts.findAll(orgId)).isEmpty();
        verify(organizationService, never()).updateSettings(any(), any());
    }

    @Test
    void rejectsMissingSpecificationReference() throws Exception {
        Path catalog = tempDir.resolve("catalog.json");
        Files.writeString(catalog, """
                [{"number":"СБ-0001","name":"Насос","type":"ASSEMBLY"}]
                """);
        Path specs = tempDir.resolve("specifications.json");
        Files.writeString(specs, """
                [{"assemblyNumber":"СБ-0001","rows":[{"type":"PART","number":"Д-9999","quantity":1}]}]
                """);

        assertThatThrownBy(() -> seeder(true, catalog, specs).run())
                .isInstanceOf(IllegalStateException.class);
    }
}
