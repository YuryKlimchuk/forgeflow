package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationService;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationView;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тонкий REST-адаптер над {@link RequirementCalculationService}.
 */
@RestController
@RequestMapping("/api/v1/plans/{planId}/calculations")
public class RequirementCalculationController {

    private final RequirementCalculationService calculationService;

    public RequirementCalculationController(RequirementCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementCalculationView calculate(@PathVariable UUID planId) {
        return calculationService.calculate(PlanId.of(planId));
    }

    @GetMapping
    public List<RequirementCalculationView> list(@PathVariable UUID planId) {
        return calculationService.list(PlanId.of(planId));
    }

    @GetMapping("/{calculationId}")
    public RequirementCalculationView get(@PathVariable UUID planId, @PathVariable UUID calculationId) {
        return calculationService.get(PlanId.of(planId), RequirementCalculationId.of(calculationId));
    }
}
