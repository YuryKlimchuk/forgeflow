package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Интеграционная проверка tenant-контекста REST для оборудования: организация
 * определяется аутентифицированным пользователем (не клиентским заголовком),
 * данные другой организации не раскрываются.
 */
class EquipmentRestTenantContextTest extends AbstractPostgresIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    TenantContextFilter tenantContextFilter;

    @Autowired
    OrganizationContext organizationContext;

    @Autowired
    EquipmentRepository equipmentRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(tenantContextFilter)
                .build();
    }

    @Test
    void contextIsClearedAfterEquipmentRequest() throws Exception {
        mockMvc.perform(get("/api/v1/equipment").with(demoUserJwt()))
                .andExpect(status().isOk());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void foreignEquipmentReturns404AndClientHeaderIsIgnored() throws Exception {
        OrganizationId foreignOrg = OrganizationId.generate();
        Equipment equipment = Equipment.create(
                EquipmentId.generate(), "С-001", "Чужой станок", "Токарный",
                foreignOrg, Instant.now(), Instant.now());
        equipmentRepository.save(equipment);

        mockMvc.perform(get("/api/v1/equipment/{id}", equipment.getId().value())
                        .with(demoUserJwt())
                        .header("X-Organization-Id", foreignOrg.toString()))
                .andExpect(status().isNotFound());

        assertThat(organizationContext.currentOrganizationId()).isEmpty();
    }

    @Test
    void foreignRecordMissingForRandomId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/equipment/{id}", UUID.randomUUID()).with(demoUserJwt()))
                .andExpect(status().isNotFound());
    }
}
