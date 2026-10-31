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
import com.hydroyura.forgeflow.catalog.api.CreateStandartRequest;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.application.DuplicateStandartNumberException;
import com.hydroyura.forgeflow.catalog.application.StandartNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StandartControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private StandartService standartService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        standartService = mock(StandartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StandartController(standartService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void get_returnsStandart() throws Exception {
        StandartView view = new StandartView(StandartId.generate(), "С-1", "Стандартное изделие", NOW, NOW);
        when(standartService.get(any(StandartId.class))).thenReturn(view);

        mockMvc.perform(get("/api/v1/standarts/{id}", view.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("С-1"))
                .andExpect(jsonPath("$.name").value("Стандартное изделие"));
    }

    @Test
    void list_returnsStandarts() throws Exception {
        StandartView view = new StandartView(StandartId.generate(), "С-1", "Стандартное изделие", NOW, NOW);
        when(standartService.list(any())).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/standarts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("С-1"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        StandartView view = new StandartView(StandartId.generate(), "С-1", "Стандартное изделие", NOW, NOW);
        when(standartService.create(any(CreateStandartRequest.class))).thenReturn(view);

        mockMvc.perform(post("/api/v1/standarts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"С-1\",\"name\":\"Стандартное изделие\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("С-1"));
    }

    @Test
    void rename_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/standarts/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/standarts/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(standartService.get(any(StandartId.class))).thenThrow(new StandartNotFoundException(StandartId.generate()));

        mockMvc.perform(get("/api/v1/standarts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        when(standartService.create(any(CreateStandartRequest.class))).thenThrow(new DuplicateStandartNumberException("С-1"));

        mockMvc.perform(post("/api/v1/standarts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"С-1\",\"name\":\"Стандартное изделие\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        when(standartService.create(any(CreateStandartRequest.class)))
                .thenThrow(new IllegalArgumentException("name must not be blank"));

        mockMvc.perform(post("/api/v1/standarts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"С-1\",\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
