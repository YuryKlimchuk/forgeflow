package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.RestExceptionHandler;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.CreatePlanRequest;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.PlanService;
import com.hydroyura.forgeflow.planning.api.PlanView;
import com.hydroyura.forgeflow.planning.domain.DuplicatePlanNameException;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlanControllerTest {

    private PlanService planService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        planService = mock(PlanService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlanController(planService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        PlanId id = PlanId.generate();
        when(planService.create(any(CreatePlanRequest.class)))
                .thenReturn(new PlanView(id, "Декабрь 2027", 2027, 12, List.of()));

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Декабрь 2027\",\"year\":2027,\"month\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Декабрь 2027"))
                .andExpect(jsonPath("$.month").value(12));
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        doThrow(new DuplicatePlanNameException("Декабрь 2027"))
                .when(planService).create(any(CreatePlanRequest.class));

        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Декабрь 2027\",\"year\":2027,\"month\":12}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void get_missing_returns404() throws Exception {
        when(planService.get(any(PlanId.class))).thenThrow(new PlanNotFoundException(PlanId.generate()));

        mockMvc.perform(get("/api/v1/plans/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void addItem_returnsCreated() throws Exception {
        AssemblyId assemblyId = AssemblyId.generate();
        mockMvc.perform(post("/api/v1/plans/{id}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assemblyId\":{\"value\":\"" + assemblyId.value() + "\"},\"quantity\":2}"))
                .andExpect(status().isCreated());
    }

    @Test
    void changeQuantity_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/plans/{id}/items/{assemblyId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void flat_returnsList() throws Exception {
        when(planService.flatten(any(PlanId.class)))
                .thenReturn(List.of(new FlatBomItem("Д-0001", "Втулка", 10)));

        mockMvc.perform(get("/api/v1/plans/{id}/flat", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("Д-0001"))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }
}
