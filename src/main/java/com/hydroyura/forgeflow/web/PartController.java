package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.CreatePartRequest;
import com.hydroyura.forgeflow.catalog.api.PartFilter;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
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
 * Тонкий REST-адаптер над {@link PartService}. Бизнес-правила — в application-слое.
 */
@RestController
@RequestMapping("/api/v1/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping("/{id}")
    public PartView get(@PathVariable UUID id) {
        return partService.get(PartId.of(id));
    }

    @GetMapping
    public List<PartView> list() {
        return partService.list(new PartFilter());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartView create(@RequestBody CreatePartRequest request) {
        return partService.create(request);
    }

    @PutMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable UUID id, @RequestBody RenamePartRequest request) {
        partService.rename(PartId.of(id), request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        partService.delete(PartId.of(id));
    }
}
