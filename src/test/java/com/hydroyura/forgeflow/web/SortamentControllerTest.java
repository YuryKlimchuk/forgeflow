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
import com.hydroyura.forgeflow.directories.api.CreateSortamentRequest;
import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.SortamentService;
import com.hydroyura.forgeflow.directories.api.SortamentView;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.api.UpdateSortamentRequest;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SortamentControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private SortamentService sortamentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sortamentService = mock(SortamentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SortamentController(sortamentService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        SortamentId id = SortamentId.generate();
        when(sortamentService.create(any(CreateSortamentRequest.class))).thenReturn(
                new SortamentView(id, "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм", NOW, NOW));

        mockMvc.perform(post("/api/v1/sortaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"Круг В-30\",\"name\":\"Круг горячекатаный В-30\",\"standardType\":\"GOST\",\"standard\":\"ГОСТ 2590-2006\",\"unit\":\"мм\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("Круг В-30"))
                .andExpect(jsonPath("$.standardType").value("GOST"));
    }

    @Test
    void list_returnsOk() throws Exception {
        when(sortamentService.list()).thenReturn(List.of(
                new SortamentView(SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм", NOW, NOW)));

        mockMvc.perform(get("/api/v1/sortaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("Круг В-30"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(sortamentService.get(any(SortamentId.class))).thenReturn(
                new SortamentView(SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30", StandardType.GOST, "ГОСТ 2590-2006", "мм", NOW, NOW));

        mockMvc.perform(get("/api/v1/sortaments/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("Круг В-30"));
    }

    @Test
    void update_returnsNoContentAndCallsServiceWithoutNumber() throws Exception {
        mockMvc.perform(put("/api/v1/sortaments/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\",\"standardType\":\"DIN\",\"standard\":\"DIN EN 10210\",\"unit\":\"см\"}"))
                .andExpect(status().isNoContent());

        verify(sortamentService).update(any(SortamentId.class), eq(new UpdateSortamentRequest("Новое имя", StandardType.DIN, "DIN EN 10210", "см")));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/sortaments/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(sortamentService).delete(any(SortamentId.class));
    }

    @Test
    void notFound_returns404() throws Exception {
        when(sortamentService.get(any(SortamentId.class)))
                .thenThrow(new SortamentNotFoundException(SortamentId.generate()));

        mockMvc.perform(get("/api/v1/sortaments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void duplicateNumber_returns409() throws Exception {
        when(sortamentService.create(any(CreateSortamentRequest.class)))
                .thenThrow(new DuplicateSortamentNumberException("Круг В-30"));

        mockMvc.perform(post("/api/v1/sortaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"Круг В-30\",\"name\":\"Круг горячекатаный В-30\",\"standardType\":\"GOST\",\"standard\":\"ГОСТ 2590-2006\",\"unit\":\"мм\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void invalidArguments_returns400() throws Exception {
        when(sortamentService.create(any(CreateSortamentRequest.class)))
                .thenThrow(new IllegalArgumentException("number must not be blank"));

        mockMvc.perform(post("/api/v1/sortaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\" \",\"name\":\"Круг горячекатаный В-30\",\"standardType\":\"GOST\",\"standard\":\"ГОСТ 2590-2006\",\"unit\":\"мм\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new SortamentNotFoundException(SortamentId.generate()))
                .when(sortamentService).delete(any(SortamentId.class));

        mockMvc.perform(delete("/api/v1/sortaments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
