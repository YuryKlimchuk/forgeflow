package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.RestExceptionHandler;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.specifications.api.AddRowRequest;
import com.hydroyura.forgeflow.specifications.api.AddSubstituteRequest;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.api.ChangeQuantityRequest;
import com.hydroyura.forgeflow.specifications.api.CopySpecificationRequest;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import com.hydroyura.forgeflow.specifications.api.RemoveRowRequest;
import com.hydroyura.forgeflow.specifications.api.RemoveSubstituteRequest;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import com.hydroyura.forgeflow.specifications.domain.BomCycleException;
import com.hydroyura.forgeflow.specifications.domain.DuplicateItemException;
import com.hydroyura.forgeflow.specifications.domain.DuplicateSubstituteException;
import com.hydroyura.forgeflow.specifications.domain.InvalidSubstituteException;
import com.hydroyura.forgeflow.specifications.domain.RowNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.SelfReferenceException;
import com.hydroyura.forgeflow.specifications.domain.SubstituteNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpecificationControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private SpecificationService specificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        specificationService = mock(SpecificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SpecificationController(specificationService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void get_returnsRows() throws Exception {
        SpecificationRowView row = new SpecificationRowView(
                new CatalogItemReference(CatalogItemType.PART, ITEM_ID), 3, false);
        when(specificationService.get(any(AssemblyId.class))).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].item.type").value("PART"))
                .andExpect(jsonPath("$[0].quantity").value(3))
                .andExpect(jsonPath("$[0].hasSubstitutes").value(false));
    }

    @Test
    void addRow_returnsCreated() throws Exception {
        SpecificationRowView row = new SpecificationRowView(
                new CatalogItemReference(CatalogItemType.PART, ITEM_ID), 3, false);
        when(specificationService.addRow(any(AssemblyId.class), any(AddRowRequest.class))).thenReturn(row);

        mockMvc.perform(post("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item.type").value("PART"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void changeQuantity_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},\"quantity\":5}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeRow_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"}}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void copy_returnsCreated() throws Exception {
        AssemblyId newAssemblyId = AssemblyId.generate();
        when(specificationService.copy(any(AssemblyId.class), any(CopySpecificationRequest.class)))
                .thenReturn(new AssemblyView(newAssemblyId, "B-1", "Новая", NOW, NOW));

        mockMvc.perform(post("/api/v1/specifications/{sourceAssemblyId}/copy", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"B-1\",\"name\":\"Новая\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("B-1"));
    }

    @Test
    void selfReference_returns400() throws Exception {
        doThrow(new SelfReferenceException(AssemblyId.generate()))
                .when(specificationService).addRow(any(AssemblyId.class), any(AddRowRequest.class));

        mockMvc.perform(post("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"ASSEMBLY\",\"id\":\"" + ITEM_ID + "\"},\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void duplicate_returns409() throws Exception {
        doThrow(new DuplicateItemException(new CatalogItemReference(CatalogItemType.PART, ITEM_ID)))
                .when(specificationService).addRow(any(AssemblyId.class), any(AddRowRequest.class));

        mockMvc.perform(post("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void missingRow_returns404() throws Exception {
        doThrow(new RowNotFoundException(new CatalogItemReference(CatalogItemType.PART, ITEM_ID)))
                .when(specificationService).changeQuantity(any(AssemblyId.class), any(ChangeQuantityRequest.class));

        mockMvc.perform(put("/api/v1/specifications/{assemblyId}/rows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},\"quantity\":5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void flat_returnsFlatList() throws Exception {
        when(specificationService.flatten(any(AssemblyId.class)))
                .thenReturn(List.of(new FlatBomItem("Д-0001", "Втулка", 11)));

        mockMvc.perform(get("/api/v1/specifications/{assemblyId}/flat", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("Д-0001"))
                .andExpect(jsonPath("$[0].quantity").value(11));
    }

    @Test
    void flat_cycle_returns409() throws Exception {
        when(specificationService.flatten(any(AssemblyId.class)))
                .thenThrow(new BomCycleException(AssemblyId.generate()));

        mockMvc.perform(get("/api/v1/specifications/{assemblyId}/flat", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("bom_cycle"));
    }

    @Test
    void listSubstitutes_returnsList() throws Exception {
        CatalogItemReference substitute = new CatalogItemReference(CatalogItemType.PART, ITEM_ID);
        when(specificationService.listSubstitutes(any(AssemblyId.class), any(CatalogItemReference.class)))
                .thenReturn(List.of(substitute));

        mockMvc.perform(get("/api/v1/specifications/{assemblyId}/substitutes", UUID.randomUUID())
                        .param("itemType", "PART")
                        .param("itemId", ITEM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PART"))
                .andExpect(jsonPath("$[0].id").value(ITEM_ID.toString()));
    }

    @Test
    void addSubstitute_returnsCreated() throws Exception {
        CatalogItemReference substitute = new CatalogItemReference(CatalogItemType.PART, ITEM_ID);
        when(specificationService.addSubstitute(any(AssemblyId.class), any(AddSubstituteRequest.class)))
                .thenReturn(substitute);

        mockMvc.perform(post("/api/v1/specifications/{assemblyId}/substitutes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},"
                                + "\"substituteRef\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PART"));
    }

    @Test
    void removeSubstitute_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/specifications/{assemblyId}/substitutes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},"
                                + "\"substituteRef\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"}}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addSubstitute_invalid_returns400() throws Exception {
        doThrow(new InvalidSubstituteException(
                new CatalogItemReference(CatalogItemType.PART, ITEM_ID),
                new CatalogItemReference(CatalogItemType.ASSEMBLY, ITEM_ID)))
                .when(specificationService).addSubstitute(any(AssemblyId.class), any(AddSubstituteRequest.class));

        mockMvc.perform(post("/api/v1/specifications/{assemblyId}/substitutes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},"
                                + "\"substituteRef\":{\"type\":\"ASSEMBLY\",\"id\":\"" + ITEM_ID + "\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void addSubstitute_duplicate_returns409() throws Exception {
        doThrow(new DuplicateSubstituteException(new CatalogItemReference(CatalogItemType.PART, ITEM_ID)))
                .when(specificationService).addSubstitute(any(AssemblyId.class), any(AddSubstituteRequest.class));

        mockMvc.perform(post("/api/v1/specifications/{assemblyId}/substitutes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},"
                                + "\"substituteRef\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void removeSubstitute_missing_returns404() throws Exception {
        doThrow(new SubstituteNotFoundException(new CatalogItemReference(CatalogItemType.PART, ITEM_ID)))
                .when(specificationService).removeSubstitute(any(AssemblyId.class), any(RemoveSubstituteRequest.class));

        mockMvc.perform(delete("/api/v1/specifications/{assemblyId}/substitutes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"},"
                                + "\"substituteRef\":{\"type\":\"PART\",\"id\":\"" + ITEM_ID + "\"}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
