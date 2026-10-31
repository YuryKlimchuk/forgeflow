package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.AssemblyFilter;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
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
 * Тонкий REST-адаптер над {@link AssemblyService}.
 */
@RestController
@RequestMapping("/api/v1/assemblies")
public class AssemblyController {

    private final AssemblyService assemblyService;

    public AssemblyController(AssemblyService assemblyService) {
        this.assemblyService = assemblyService;
    }

    @GetMapping("/{id}")
    public AssemblyView get(@PathVariable UUID id) {
        return assemblyService.get(AssemblyId.of(id));
    }

    @GetMapping
    public List<AssemblyView> list() {
        return assemblyService.list(new AssemblyFilter());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssemblyView create(@RequestBody CreateAssemblyRequest request) {
        return assemblyService.create(request);
    }

    @PutMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable UUID id, @RequestBody RenameRequest request) {
        assemblyService.rename(AssemblyId.of(id), request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        assemblyService.delete(AssemblyId.of(id));
    }
}
