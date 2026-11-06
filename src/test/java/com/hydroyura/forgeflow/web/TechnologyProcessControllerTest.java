package com.hydroyura.forgeflow.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.application.PartNotFoundException;
import com.hydroyura.forgeflow.technology.api.AddOperationRequest;
import com.hydroyura.forgeflow.technology.api.CreateTechnologyProcessRequest;
import com.hydroyura.forgeflow.technology.api.OperationView;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.api.TechnologyService;
import com.hydroyura.forgeflow.technology.domain.DuplicateOperationException;
import com.hydroyura.forgeflow.technology.domain.DuplicateTechnologyProcessNumberException;
import com.hydroyura.forgeflow.technology.domain.InvalidDefaultProcessStateException;
import com.hydroyura.forgeflow.technology.domain.OperationNotFoundException;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TechnologyProcessControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private TechnologyService technologyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        technologyService = mock(TechnologyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TechnologyProcessController(technologyService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        PartId partId = PartId.generate();
        TechnologyProcessView view = new TechnologyProcessView(
                TechnologyProcessId.generate(), "TP-1", "Фрезеровка", partId, false,
                NOW, NOW, List.of());
        when(technologyService.create(any(CreateTechnologyProcessRequest.class))).thenReturn(view);

        mockMvc.perform(post("/api/v1/technology-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"TP-1\",\"name\":\"Фрезеровка\",\"partId\":{\"value\":\"" + partId.value() + "\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("TP-1"))
                .andExpect(jsonPath("$.defaultProcess").value(false));
    }

    @Test
    void list_returnsOk() throws Exception {
        PartId partId = PartId.generate();
        when(technologyService.list(isNull())).thenReturn(List.of(
                new TechnologyProcessView(TechnologyProcessId.generate(), "TP-1", "Фрезеровка",
                        partId, false, NOW, NOW, List.of())));

        mockMvc.perform(get("/api/v1/technology-processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("TP-1"));
    }

    @Test
    void list_withPartId_returnsOk() throws Exception {
        PartId partId = PartId.generate();
        when(technologyService.list(partId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/technology-processes").param("partId", partId.value().toString()))
                .andExpect(status().isOk());

        verify(technologyService).list(partId);
    }

    @Test
    void get_returnsOk() throws Exception {
        PartId partId = PartId.generate();
        when(technologyService.get(any(TechnologyProcessId.class))).thenReturn(
                new TechnologyProcessView(TechnologyProcessId.generate(), "TP-1", "Фрезеровка",
                        partId, false, NOW, NOW, List.of()));

        mockMvc.perform(get("/api/v1/technology-processes/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("TP-1"));
    }

    @Test
    void rename_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/technology-processes/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\"}"))
                .andExpect(status().isNoContent());

        verify(technologyService).rename(any(TechnologyProcessId.class), eq("Новое имя"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/technology-processes/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void addOperation_returnsCreated() throws Exception {
        when(technologyService.addOperation(any(TechnologyProcessId.class), any(AddOperationRequest.class)))
                .thenReturn(new OperationView(10, "Сверление"));

        mockMvc.perform(post("/api/v1/technology-processes/{id}/operations", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":10,\"name\":\"Сверление\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(10))
                .andExpect(jsonPath("$.name").value("Сверление"));
    }

    @Test
    void removeOperation_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/technology-processes/{id}/operations/{number}", UUID.randomUUID(), 10))
                .andExpect(status().isNoContent());

        verify(technologyService).removeOperation(any(TechnologyProcessId.class), eq(10));
    }

    @Test
    void markAsDefault_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/technology-processes/{id}/default", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unmarkDefault_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/technology-processes/{id}/default", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void notFound_returns404() throws Exception {
        when(technologyService.get(any(TechnologyProcessId.class)))
                .thenThrow(new TechnologyProcessNotFoundException(TechnologyProcessId.generate()));

        mockMvc.perform(get("/api/v1/technology-processes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void operationNotFound_returns404() throws Exception {
        doThrow(new OperationNotFoundException(99))
                .when(technologyService).removeOperation(any(TechnologyProcessId.class), anyInt());

        mockMvc.perform(delete("/api/v1/technology-processes/{id}/operations/{number}", UUID.randomUUID(), 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void duplicateNumber_returns409() throws Exception {
        when(technologyService.create(any(CreateTechnologyProcessRequest.class)))
                .thenThrow(new DuplicateTechnologyProcessNumberException("TP-1"));

        mockMvc.perform(post("/api/v1/technology-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"TP-1\",\"name\":\"Фрезеровка\",\"partId\":{\"value\":\"" + UUID.randomUUID() + "\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void duplicateOperation_returns409() throws Exception {
        when(technologyService.addOperation(any(TechnologyProcessId.class), any(AddOperationRequest.class)))
                .thenThrow(new DuplicateOperationException(10));

        mockMvc.perform(post("/api/v1/technology-processes/{id}/operations", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":10,\"name\":\"Сверление\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void invalidDefaultState_returns409() throws Exception {
        doThrow(new InvalidDefaultProcessStateException(PartId.generate()))
                .when(technologyService).unmarkDefault(any(TechnologyProcessId.class));

        mockMvc.perform(delete("/api/v1/technology-processes/{id}/default", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("invalid_state"));
    }

    @Test
    void invalidArguments_returns400() throws Exception {
        when(technologyService.create(any(CreateTechnologyProcessRequest.class)))
                .thenThrow(new IllegalArgumentException("number must not be blank"));

        mockMvc.perform(post("/api/v1/technology-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\" \",\"name\":\"Фрезеровка\",\"partId\":{\"value\":\"" + UUID.randomUUID() + "\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void partNotFound_returns404() throws Exception {
        when(technologyService.create(any(CreateTechnologyProcessRequest.class)))
                .thenThrow(new PartNotFoundException(PartId.generate()));

        mockMvc.perform(post("/api/v1/technology-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"TP-1\",\"name\":\"Фрезеровка\",\"partId\":{\"value\":\"" + UUID.randomUUID() + "\"}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
