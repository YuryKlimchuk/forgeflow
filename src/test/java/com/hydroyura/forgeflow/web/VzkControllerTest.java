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
import com.hydroyura.forgeflow.catalog.api.CreateVzkRequest;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.catalog.application.DuplicateVzkNumberException;
import com.hydroyura.forgeflow.catalog.application.VzkNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VzkControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private VzkService vzkService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        vzkService = mock(VzkService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VzkController(vzkService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void get_returnsVzk() throws Exception {
        VzkView view = new VzkView(VzkId.generate(), "В-1", "Группа ВЗК", NOW, NOW);
        when(vzkService.get(any(VzkId.class))).thenReturn(view);

        mockMvc.perform(get("/api/v1/vzks/{id}", view.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("В-1"))
                .andExpect(jsonPath("$.name").value("Группа ВЗК"));
    }

    @Test
    void list_returnsVzks() throws Exception {
        VzkView view = new VzkView(VzkId.generate(), "В-1", "Группа ВЗК", NOW, NOW);
        when(vzkService.list(any())).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/vzks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("В-1"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        VzkView view = new VzkView(VzkId.generate(), "В-1", "Группа ВЗК", NOW, NOW);
        when(vzkService.create(any(CreateVzkRequest.class))).thenReturn(view);

        mockMvc.perform(post("/api/v1/vzks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"В-1\",\"name\":\"Группа ВЗК\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("В-1"));
    }

    @Test
    void rename_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/vzks/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/vzks/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(vzkService.get(any(VzkId.class))).thenThrow(new VzkNotFoundException(VzkId.generate()));

        mockMvc.perform(get("/api/v1/vzks/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        when(vzkService.create(any(CreateVzkRequest.class))).thenThrow(new DuplicateVzkNumberException("В-1"));

        mockMvc.perform(post("/api/v1/vzks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"В-1\",\"name\":\"Группа ВЗК\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        when(vzkService.create(any(CreateVzkRequest.class)))
                .thenThrow(new IllegalArgumentException("name must not be blank"));

        mockMvc.perform(post("/api/v1/vzks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"В-1\",\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
