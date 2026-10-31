package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.CreateVzkRequest;
import com.hydroyura.forgeflow.catalog.api.VzkFilter;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.api.VzkView;
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
 * Тонкий REST-адаптер над {@link VzkService}.
 */
@RestController
@RequestMapping("/api/v1/vzks")
public class VzkController {

    private final VzkService vzkService;

    public VzkController(VzkService vzkService) {
        this.vzkService = vzkService;
    }

    @GetMapping("/{id}")
    public VzkView get(@PathVariable UUID id) {
        return vzkService.get(VzkId.of(id));
    }

    @GetMapping
    public List<VzkView> list() {
        return vzkService.list(new VzkFilter());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VzkView create(@RequestBody CreateVzkRequest request) {
        return vzkService.create(request);
    }

    @PutMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable UUID id, @RequestBody RenameRequest request) {
        vzkService.rename(VzkId.of(id), request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        vzkService.delete(VzkId.of(id));
    }
}
