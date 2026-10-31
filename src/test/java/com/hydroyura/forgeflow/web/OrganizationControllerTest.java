package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.RestExceptionHandler;
import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.application.OrganizationNotFoundException;
import com.hydroyura.forgeflow.organizations.application.OwnerRequiredException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrganizationControllerTest {

    private OrganizationService organizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        organizationService = mock(OrganizationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrganizationController(organizationService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void updateSettings_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/{id}/settings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partNumberPattern\":\"PART-[0-9]+\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateSettings_byNonOwner_returns403() throws Exception {
        doThrow(new OwnerRequiredException("Only the owner can perform this operation"))
                .when(organizationService).updateSettings(any(), any());

        mockMvc.perform(put("/api/v1/organizations/{id}/settings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void updateSettings_invalidRegex_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Invalid part number pattern"))
                .when(organizationService).updateSettings(any(), any());

        mockMvc.perform(put("/api/v1/organizations/{id}/settings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partNumberPattern\":\"([unclosed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void updateSettings_unknownOrganization_returns404() throws Exception {
        doThrow(new OrganizationNotFoundException("Organization not found"))
                .when(organizationService).updateSettings(any(), any());

        mockMvc.perform(put("/api/v1/organizations/{id}/settings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
