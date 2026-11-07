package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Интеграционная проверка tenant-контекста REST для справочников: организация
 * определяется аутентифицированным пользователем (не клиентским заголовком),
 * данные другой организации не раскрываются.
 */
class DirectoriesRestTenantContextTest extends AbstractPostgresIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    TenantContextFilter tenantContextFilter;

    @Autowired
    OrganizationContext organizationContext;

    @Autowired
    MaterialRepository materialRepository;

    @Autowired
    SortamentRepository sortamentRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(tenantContextFilter)
                .build();
    }

    @Test
    void contextIsClearedAfterDirectoriesRequest() throws Exception {
        mockMvc.perform(get("/api/v1/materials").with(demoUserJwt()))
                .andExpect(status().isOk());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void foreignMaterialReturns404AndClientHeaderIsIgnored() throws Exception {
        OrganizationId foreignOrg = OrganizationId.generate();
        Material material = Material.create(
                MaterialId.generate(), "45", "Чужая сталь", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                foreignOrg, Instant.now(), Instant.now());
        materialRepository.save(material);

        mockMvc.perform(get("/api/v1/materials/{id}", material.getId().value())
                        .with(demoUserJwt())
                        .header("X-Organization-Id", foreignOrg.toString()))
                .andExpect(status().isNotFound());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void foreignSortamentReturns404AndClientHeaderIsIgnored() throws Exception {
        OrganizationId foreignOrg = OrganizationId.generate();
        Sortament sortament = Sortament.create(
                SortamentId.generate(), "Круг В-30", "Чужой круг", StandardType.GOST, "ГОСТ 2590-2006", "мм",
                foreignOrg, Instant.now(), Instant.now());
        sortamentRepository.save(sortament);

        mockMvc.perform(get("/api/v1/sortaments/{id}", sortament.getId().value())
                        .with(demoUserJwt())
                        .header("X-Organization-Id", foreignOrg.toString()))
                .andExpect(status().isNotFound());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void foreignRecordMissingForRandomId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/materials/{id}", UUID.randomUUID()).with(demoUserJwt()))
                .andExpect(status().isNotFound());
    }
}
