package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
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
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.catalog.application.AssemblyNotFoundException;
import com.hydroyura.forgeflow.catalog.application.DuplicateAssemblyNumberException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AssemblyControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AssemblyService assemblyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        assemblyService = mock(AssemblyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AssemblyController(assemblyService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void get_returnsAssembly() throws Exception {
        AssemblyView view = new AssemblyView(AssemblyId.generate(), "А-1", "Узел", NOW, NOW);
        when(assemblyService.get(any(AssemblyId.class))).thenReturn(view);

        mockMvc.perform(get("/api/v1/assemblies/{id}", view.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("А-1"))
                .andExpect(jsonPath("$.name").value("Узел"));
    }

    @Test
    void list_returnsAssemblies() throws Exception {
        AssemblyView view = new AssemblyView(AssemblyId.generate(), "А-1", "Узел", NOW, NOW);
        when(assemblyService.list(any())).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/assemblies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("А-1"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        AssemblyView view = new AssemblyView(AssemblyId.generate(), "А-1", "Узел", NOW, NOW);
        when(assemblyService.create(any(CreateAssemblyRequest.class))).thenReturn(view);

        mockMvc.perform(post("/api/v1/assemblies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"А-1\",\"name\":\"Узел\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("А-1"));
    }

    @Test
    void rename_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/assemblies/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/assemblies/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(assemblyService.get(any(AssemblyId.class))).thenThrow(new AssemblyNotFoundException(AssemblyId.generate()));

        mockMvc.perform(get("/api/v1/assemblies/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        when(assemblyService.create(any(CreateAssemblyRequest.class))).thenThrow(new DuplicateAssemblyNumberException("А-1"));

        mockMvc.perform(post("/api/v1/assemblies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"А-1\",\"name\":\"Узел\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        when(assemblyService.create(any(CreateAssemblyRequest.class)))
                .thenThrow(new IllegalArgumentException("name must not be blank"));

        mockMvc.perform(post("/api/v1/assemblies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"А-1\",\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
