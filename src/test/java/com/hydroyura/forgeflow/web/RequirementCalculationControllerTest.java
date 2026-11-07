package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.RestExceptionHandler;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationService;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationView;
import com.hydroyura.forgeflow.planning.api.RequirementItemView;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationNotFoundException;
import com.hydroyura.forgeflow.planning.domain.RequirementCycleException;
import com.hydroyura.forgeflow.planning.domain.RequirementQuantityOverflowException;
import com.hydroyura.forgeflow.planning.domain.SpecificationItemNotFoundException;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RequirementCalculationControllerTest {

    private static final Instant EXECUTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private RequirementCalculationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(RequirementCalculationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RequirementCalculationController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private static RequirementCalculationView view(PlanId planId) {
        AssemblyId source = AssemblyId.generate();
        RequirementItemView item = new RequirementItemView(
                CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Втулка", 3,
                NeedType.MANUFACTURE, "шт", List.of(source));
        return new RequirementCalculationView(
                RequirementCalculationId.generate(), planId, EXECUTED_AT, List.of(item));
    }

    @Test
    void calculate_returnsCreatedWithBody() throws Exception {
        PlanId planId = PlanId.generate();
        when(service.calculate(planId)).thenReturn(view(planId));

        mockMvc.perform(post("/api/v1/plans/{planId}/calculations", planId.value()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId.value").value(planId.value().toString()))
                .andExpect(jsonPath("$.executedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].type").value("PART"))
                .andExpect(jsonPath("$.items[0].needType").value("MANUFACTURE"))
                .andExpect(jsonPath("$.items[0].unit").value("шт"))
                .andExpect(jsonPath("$.items[0].sources[0].value").exists());
    }

    @Test
    void list_returnsOk() throws Exception {
        PlanId planId = PlanId.generate();
        when(service.list(planId)).thenReturn(List.of(view(planId)));

        mockMvc.perform(get("/api/v1/plans/{planId}/calculations", planId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planId.value").value(planId.value().toString()));
    }

    @Test
    void get_returnsOk() throws Exception {
        PlanId planId = PlanId.generate();
        RequirementCalculationId calcId = RequirementCalculationId.generate();
        when(service.get(planId, calcId)).thenReturn(view(planId));

        mockMvc.perform(get("/api/v1/plans/{planId}/calculations/{calcId}", planId.value(), calcId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId.value").value(planId.value().toString()));
    }

    @Test
    void calculate_missingPlan_returns404() throws Exception {
        when(service.calculate(any(PlanId.class))).thenThrow(new PlanNotFoundException(PlanId.generate()));

        mockMvc.perform(post("/api/v1/plans/{planId}/calculations", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void list_missingPlan_returns404() throws Exception {
        when(service.list(any(PlanId.class))).thenThrow(new PlanNotFoundException(PlanId.generate()));

        mockMvc.perform(get("/api/v1/plans/{planId}/calculations", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void get_missingPlan_returns404() throws Exception {
        when(service.get(any(PlanId.class), any(RequirementCalculationId.class)))
                .thenThrow(new PlanNotFoundException(PlanId.generate()));

        mockMvc.perform(get("/api/v1/plans/{planId}/calculations/{calcId}",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void get_missingCalculation_returns404() throws Exception {
        when(service.get(any(PlanId.class), any(RequirementCalculationId.class)))
                .thenThrow(new RequirementCalculationNotFoundException(RequirementCalculationId.generate()));

        mockMvc.perform(get("/api/v1/plans/{planId}/calculations/{calcId}",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void calculate_danglingNestedAssembly_returns404WithCode() throws Exception {
        UUID nestedId = UUID.randomUUID();
        when(service.calculate(any(PlanId.class)))
                .thenThrow(new SpecificationItemNotFoundException(
                        new CatalogItemReference(CatalogItemType.ASSEMBLY, nestedId)));

        mockMvc.perform(post("/api/v1/plans/{planId}/calculations", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("specification_item_not_found"))
                .andExpect(jsonPath("$.itemType").value("ASSEMBLY"))
                .andExpect(jsonPath("$.itemId").value(nestedId.toString()));
    }

    @Test
    void calculate_cycle_returns409() throws Exception {
        when(service.calculate(any(PlanId.class)))
                .thenThrow(new RequirementCycleException(AssemblyId.generate()));

        mockMvc.perform(post("/api/v1/plans/{planId}/calculations", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("bom_cycle"));
    }

    @Test
    void calculate_overflow_returns400() throws Exception {
        when(service.calculate(any(PlanId.class)))
                .thenThrow(new RequirementQuantityOverflowException("quantity overflow"));

        mockMvc.perform(post("/api/v1/plans/{planId}/calculations", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("quantity_overflow"));
    }
}
