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
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.api.CreateBuyRequest;
import com.hydroyura.forgeflow.catalog.application.BuyNotFoundException;
import com.hydroyura.forgeflow.catalog.application.DuplicateBuyNumberException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BuyControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private BuyService buyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        buyService = mock(BuyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BuyController(buyService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void get_returnsBuy() throws Exception {
        BuyView view = new BuyView(BuyId.generate(), "Б-1", "Покупное изделие", NOW, NOW);
        when(buyService.get(any(BuyId.class))).thenReturn(view);

        mockMvc.perform(get("/api/v1/buys/{id}", view.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("Б-1"))
                .andExpect(jsonPath("$.name").value("Покупное изделие"));
    }

    @Test
    void list_returnsBuys() throws Exception {
        BuyView view = new BuyView(BuyId.generate(), "Б-1", "Покупное изделие", NOW, NOW);
        when(buyService.list(any())).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/buys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("Б-1"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        BuyView view = new BuyView(BuyId.generate(), "Б-1", "Покупное изделие", NOW, NOW);
        when(buyService.create(any(CreateBuyRequest.class))).thenReturn(view);

        mockMvc.perform(post("/api/v1/buys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"Б-1\",\"name\":\"Покупное изделие\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("Б-1"));
    }

    @Test
    void rename_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/v1/buys/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое имя\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/buys/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(buyService.get(any(BuyId.class))).thenThrow(new BuyNotFoundException(BuyId.generate()));

        mockMvc.perform(get("/api/v1/buys/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        when(buyService.create(any(CreateBuyRequest.class))).thenThrow(new DuplicateBuyNumberException("Б-1"));

        mockMvc.perform(post("/api/v1/buys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"Б-1\",\"name\":\"Покупное изделие\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        when(buyService.create(any(CreateBuyRequest.class)))
                .thenThrow(new IllegalArgumentException("name must not be blank"));

        mockMvc.perform(post("/api/v1/buys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"Б-1\",\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
