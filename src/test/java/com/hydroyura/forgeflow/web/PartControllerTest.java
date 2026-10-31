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
import com.hydroyura.forgeflow.catalog.api.CreatePartRequest;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.application.DuplicatePartNumberException;
import com.hydroyura.forgeflow.catalog.application.PartNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PartControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private PartService partService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        partService = mock(PartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PartController(partService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void get_returnsPart() throws Exception {
        PartView view = new PartView(PartId.generate(), "P-1", "Деталь", NOW, NOW);
        when(partService.get(any(PartId.class))).thenReturn(view);

        mockMvc.perform(get("/api/v1/parts/{id}", view.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("P-1"))
                .andExpect(jsonPath("$.name").value("Деталь"));
    }

    @Test
    void list_returnsParts() throws Exception {
        PartView view = new PartView(PartId.generate(), "P-1", "Деталь", NOW, NOW);
        when(partService.list(any())).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("P-1"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        PartView view = new PartView(PartId.generate(), "P-1", "Деталь", NOW, NOW);
        when(partService.create(any(CreatePartRequest.class))).thenReturn(view);

        mockMvc.perform(post("/api/v1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"P-1\",\"name\":\"Деталь\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("P-1"));
    }

    @Test
    void rename_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/parts/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/parts/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(partService.get(any(PartId.class))).thenThrow(new PartNotFoundException(PartId.generate()));

        mockMvc.perform(get("/api/v1/parts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        when(partService.create(any(CreatePartRequest.class))).thenThrow(new DuplicatePartNumberException("P-1"));

        mockMvc.perform(post("/api/v1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"P-1\",\"name\":\"Деталь\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        when(partService.create(any(CreatePartRequest.class)))
                .thenThrow(new IllegalArgumentException("name must not be blank"));

        mockMvc.perform(post("/api/v1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"P-1\",\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
