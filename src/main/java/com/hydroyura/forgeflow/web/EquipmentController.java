package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.technology.api.CreateEquipmentRequest;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.EquipmentService;
import com.hydroyura.forgeflow.technology.api.EquipmentView;
import com.hydroyura.forgeflow.technology.api.UpdateEquipmentRequest;
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
 * Тонкий REST-адаптер над {@link EquipmentService}.
 */
@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentView create(@RequestBody CreateEquipmentRequest request) {
        return equipmentService.create(request);
    }

    @GetMapping
    public List<EquipmentView> list() {
        return equipmentService.list();
    }

    @GetMapping("/{id}")
    public EquipmentView get(@PathVariable UUID id) {
        return equipmentService.get(EquipmentId.of(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id, @RequestBody UpdateEquipmentRequest request) {
        equipmentService.update(EquipmentId.of(id), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        equipmentService.delete(EquipmentId.of(id));
    }
}
