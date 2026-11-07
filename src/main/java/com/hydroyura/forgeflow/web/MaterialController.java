package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.directories.api.CreateMaterialRequest;
import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.MaterialService;
import com.hydroyura.forgeflow.directories.api.MaterialView;
import com.hydroyura.forgeflow.directories.api.UpdateMaterialRequest;
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
 * Тонкий REST-адаптер над {@link MaterialService}.
 */
@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialView create(@RequestBody CreateMaterialRequest request) {
        return materialService.create(request);
    }

    @GetMapping
    public List<MaterialView> list() {
        return materialService.list();
    }

    @GetMapping("/{id}")
    public MaterialView get(@PathVariable UUID id) {
        return materialService.get(MaterialId.of(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id, @RequestBody UpdateMaterialRequest request) {
        materialService.update(MaterialId.of(id), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        materialService.delete(MaterialId.of(id));
    }
}
