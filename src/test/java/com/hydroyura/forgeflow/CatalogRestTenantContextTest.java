package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Интеграционная проверка tenant-контекста REST: организация определяется
 * аутентифицированным пользователем (не клиентским заголовком), а контекст
 * очищается после любого запроса.
 */
@SpringBootTest(properties = "forgeflow.seed.enabled=false")
class CatalogRestTenantContextTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    TenantContextFilter tenantContextFilter;

    @Autowired
    OrganizationContext organizationContext;

    @Autowired
    AssemblyRepository assemblyRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(tenantContextFilter)
                .build();
    }

    @Test
    void contextIsClearedAfterSuccessfulRequest() throws Exception {
        mockMvc.perform(get("/api/v1/assemblies"))
                .andExpect(status().isOk());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void contextIsClearedAfterNotFoundRequest() throws Exception {
        mockMvc.perform(get("/api/v1/assemblies/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void foreignRecordReturns404AndClientHeaderIsIgnored() throws Exception {
        OrganizationId foreignOrg = OrganizationId.generate();
        Assembly assembly = Assembly.create(
                AssemblyId.generate(), "А-999", "Чужой узел", foreignOrg, Instant.now(), Instant.now());
        assemblyRepository.save(assembly);

        mockMvc.perform(get("/api/v1/assemblies/{id}", assembly.getId().value())
                        .header("X-Organization-Id", foreignOrg.toString()))
                .andExpect(status().isNotFound());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }
}
