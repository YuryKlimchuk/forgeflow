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
import com.hydroyura.forgeflow.technology.api.CreateEquipmentRequest;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.EquipmentService;
import com.hydroyura.forgeflow.technology.api.EquipmentView;
import com.hydroyura.forgeflow.technology.api.UpdateEquipmentRequest;
import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EquipmentControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private EquipmentService equipmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        equipmentService = mock(EquipmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EquipmentController(equipmentService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        EquipmentId id = EquipmentId.generate();
        when(equipmentService.create(any(CreateEquipmentRequest.class))).thenReturn(
                new EquipmentView(id, "С-001", "Станок токарный", "Токарный", NOW, NOW));

        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"С-001\",\"name\":\"Станок токарный\",\"type\":\"Токарный\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("С-001"))
                .andExpect(jsonPath("$.type").value("Токарный"));
    }

    @Test
    void list_returnsOk() throws Exception {
        when(equipmentService.list()).thenReturn(List.of(
                new EquipmentView(EquipmentId.generate(), "С-001", "Станок токарный", "Токарный", NOW, NOW)));

        mockMvc.perform(get("/api/v1/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("С-001"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(equipmentService.get(any(EquipmentId.class))).thenReturn(
                new EquipmentView(EquipmentId.generate(), "С-001", "Станок токарный", "Токарный", NOW, NOW));

        mockMvc.perform(get("/api/v1/equipment/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("С-001"));
    }

    @Test
    void update_returnsNoContentAndCallsServiceWithoutNumber() throws Exception {
        mockMvc.perform(put("/api/v1/equipment/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новый станок\",\"type\":\"Фрезерный\"}"))
                .andExpect(status().isNoContent());

        verify(equipmentService).update(any(EquipmentId.class), eq(new UpdateEquipmentRequest("Новый станок", "Фрезерный")));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/equipment/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(equipmentService).delete(any(EquipmentId.class));
    }

    @Test
    void notFound_returns404() throws Exception {
        when(equipmentService.get(any(EquipmentId.class)))
                .thenThrow(new EquipmentNotFoundException(EquipmentId.generate()));

        mockMvc.perform(get("/api/v1/equipment/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void duplicateNumber_returns409() throws Exception {
        when(equipmentService.create(any(CreateEquipmentRequest.class)))
                .thenThrow(new DuplicateEquipmentNumberException("С-001"));

        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"С-001\",\"name\":\"Станок токарный\",\"type\":\"Токарный\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void invalidArguments_returns400() throws Exception {
        when(equipmentService.create(any(CreateEquipmentRequest.class)))
                .thenThrow(new IllegalArgumentException("number must not be blank"));

        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\" \",\"name\":\"Станок токарный\",\"type\":\"Токарный\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new EquipmentNotFoundException(EquipmentId.generate()))
                .when(equipmentService).delete(any(EquipmentId.class));

        mockMvc.perform(delete("/api/v1/equipment/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
