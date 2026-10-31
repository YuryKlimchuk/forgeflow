package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.CreateStandartRequest;
import com.hydroyura.forgeflow.catalog.api.StandartFilter;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тонкий REST-адаптер над {@link StandartService}.
 */
@RestController
@RequestMapping("/api/v1/standarts")
public class StandartController {

    private final StandartService standartService;

    public StandartController(StandartService standartService) {
        this.standartService = standartService;
    }

    @GetMapping("/{id}")
    public StandartView get(@PathVariable UUID id) {
        return standartService.get(StandartId.of(id));
    }

    @GetMapping
    public List<StandartView> list() {
        return standartService.list(new StandartFilter());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StandartView create(@RequestBody CreateStandartRequest request) {
        return standartService.create(request);
    }

    @PutMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable UUID id, @RequestBody RenameRequest request) {
        standartService.rename(StandartId.of(id), request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        standartService.delete(StandartId.of(id));
    }
}
