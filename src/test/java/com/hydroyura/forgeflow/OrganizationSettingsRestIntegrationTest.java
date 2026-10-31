package com.hydroyura.forgeflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Сквозная проверка: владелец задаёт паттерн номера, а REST-создание детали
 * учитывает его.
 */
@SpringBootTest(properties = "forgeflow.seed.enabled=false")
class OrganizationSettingsRestIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    TenantContextFilter tenantContextFilter;

    @Autowired
    OrganizationId demoOrganizationId;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(tenantContextFilter)
                .build();
    }

    @Test
    void settingsAreAppliedToCatalogCreation() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/{id}/settings", demoOrganizationId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partNumberPattern\":\"PART-[0-9]+\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"42\",\"name\":\"Деталь\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));

        mockMvc.perform(post("/api/v1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"PART-42\",\"name\":\"Деталь\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidRegexRejectedAtSettingsUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/{id}/settings", demoOrganizationId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partNumberPattern\":\"([unclosed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
