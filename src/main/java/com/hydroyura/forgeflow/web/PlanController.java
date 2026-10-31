package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.AddPlanItemRequest;
import com.hydroyura.forgeflow.planning.api.ChangePlanItemQuantityRequest;
import com.hydroyura.forgeflow.planning.api.CreatePlanRequest;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.PlanItemView;
import com.hydroyura.forgeflow.planning.api.PlanService;
import com.hydroyura.forgeflow.planning.api.PlanView;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
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
 * Тонкий REST-адаптер над {@link PlanService}.
 */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanView create(@RequestBody CreatePlanRequest request) {
        return planService.create(request);
    }

    @GetMapping
    public List<PlanView> list() {
        return planService.list();
    }

    @GetMapping("/{id}")
    public PlanView get(@PathVariable UUID id) {
        return planService.get(PlanId.of(id));
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanItemView addItem(@PathVariable UUID id, @RequestBody AddPlanItemRequest request) {
        return planService.addItem(PlanId.of(id), request);
    }

    @PutMapping("/{id}/items/{assemblyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeQuantity(
            @PathVariable UUID id,
            @PathVariable UUID assemblyId,
            @RequestBody ChangePlanItemQuantityRequest request) {
        planService.changeQuantity(PlanId.of(id), AssemblyId.of(assemblyId), request);
    }

    @DeleteMapping("/{id}/items/{assemblyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID id, @PathVariable UUID assemblyId) {
        planService.removeItem(PlanId.of(id), AssemblyId.of(assemblyId));
    }

    @GetMapping("/{id}/flat")
    public List<FlatBomItem> flat(@PathVariable UUID id) {
        return planService.flatten(PlanId.of(id));
    }
}
