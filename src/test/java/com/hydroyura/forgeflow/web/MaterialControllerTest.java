package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hydroyura.forgeflow.RestExceptionHandler;
import com.hydroyura.forgeflow.directories.api.CreateMaterialRequest;
import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.MaterialService;
import com.hydroyura.forgeflow.directories.api.MaterialView;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.api.UpdateMaterialRequest;
import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.MaterialNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MaterialControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private MaterialService materialService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        materialService = mock(MaterialService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MaterialController(materialService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        MaterialId id = MaterialId.generate();
        when(materialService.create(any(CreateMaterialRequest.class))).thenReturn(
                new MaterialView(id, "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг", NOW, NOW));

        mockMvc.perform(post("/api/v1/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"45\",\"name\":\"Сталь 45\",\"standardType\":\"GOST\",\"standard\":\"ГОСТ 1050-2013\",\"unit\":\"кг\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("45"))
                .andExpect(jsonPath("$.standardType").value("GOST"));
    }

    @Test
    void list_returnsOk() throws Exception {
        when(materialService.list()).thenReturn(List.of(
                new MaterialView(MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг", NOW, NOW)));

        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("45"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(materialService.get(any(MaterialId.class))).thenReturn(
                new MaterialView(MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг", NOW, NOW));

        mockMvc.perform(get("/api/v1/materials/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("45"));
    }

    @Test
    void update_returnsNoContentAndCallsServiceWithoutNumber() throws Exception {
        mockMvc.perform(put("/api/v1/materials/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\",\"standardType\":\"DIN\",\"standard\":\"DIN EN 10088-2\",\"unit\":\"м\"}"))
                .andExpect(status().isNoContent());

        verify(materialService).update(any(MaterialId.class), eq(new UpdateMaterialRequest("Новое имя", StandardType.DIN, "DIN EN 10088-2", "м")));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/materials/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(materialService).delete(any(MaterialId.class));
    }

    @Test
    void notFound_returns404() throws Exception {
        when(materialService.get(any(MaterialId.class)))
                .thenThrow(new MaterialNotFoundException(MaterialId.generate()));

        mockMvc.perform(get("/api/v1/materials/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void duplicateNumber_returns409() throws Exception {
        when(materialService.create(any(CreateMaterialRequest.class)))
                .thenThrow(new DuplicateMaterialNumberException("45"));

        mockMvc.perform(post("/api/v1/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"45\",\"name\":\"Сталь 45\",\"standardType\":\"GOST\",\"standard\":\"ГОСТ 1050-2013\",\"unit\":\"кг\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void invalidArguments_returns400() throws Exception {
        when(materialService.create(any(CreateMaterialRequest.class)))
                .thenThrow(new IllegalArgumentException("number must not be blank"));

        mockMvc.perform(post("/api/v1/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\" \",\"name\":\"Сталь 45\",\"standardType\":\"GOST\",\"standard\":\"ГОСТ 1050-2013\",\"unit\":\"кг\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new MaterialNotFoundException(MaterialId.generate()))
                .when(materialService).delete(any(MaterialId.class));

        mockMvc.perform(delete("/api/v1/materials/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
