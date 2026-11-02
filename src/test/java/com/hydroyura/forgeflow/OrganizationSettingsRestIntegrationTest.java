package com.hydroyura.forgeflow;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Сквозная проверка: владелец задаёт паттерн номера, а REST-создание детали
 * учитывает его.
 */
class OrganizationSettingsRestIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    TenantContextFilter tenantContextFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(tenantContextFilter)
                .build();
    }

    @Test
    void settingsAreAppliedToCatalogCreation() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/{id}/settings", DEMO_ORGANIZATION_ID)
                        .with(demoUserJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partNumberPattern\":\"PART-[0-9]+\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/parts")
                        .with(demoUserJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"42\",\"name\":\"Деталь\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));

        mockMvc.perform(post("/api/v1/parts")
                        .with(demoUserJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"PART-42\",\"name\":\"Деталь\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidRegexRejectedAtSettingsUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/{id}/settings", DEMO_ORGANIZATION_ID)
                        .with(demoUserJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partNumberPattern\":\"([unclosed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
