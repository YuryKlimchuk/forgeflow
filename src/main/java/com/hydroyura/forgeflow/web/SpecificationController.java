package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.specifications.api.AddRowRequest;
import com.hydroyura.forgeflow.specifications.api.ChangeQuantityRequest;
import com.hydroyura.forgeflow.specifications.api.CopySpecificationRequest;
import com.hydroyura.forgeflow.specifications.api.RemoveRowRequest;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
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
 * Тонкий REST-адаптер над {@link SpecificationService}.
 */
@RestController
@RequestMapping("/api/v1/specifications")
public class SpecificationController {

    private final SpecificationService specificationService;

    public SpecificationController(SpecificationService specificationService) {
        this.specificationService = specificationService;
    }

    @GetMapping("/{assemblyId}/rows")
    public List<SpecificationRowView> get(@PathVariable UUID assemblyId) {
        return specificationService.get(AssemblyId.of(assemblyId));
    }

    @PostMapping("/{assemblyId}/rows")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecificationRowView addRow(@PathVariable UUID assemblyId, @RequestBody AddRowRequest request) {
        return specificationService.addRow(AssemblyId.of(assemblyId), request);
    }

    @PutMapping("/{assemblyId}/rows")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeQuantity(@PathVariable UUID assemblyId, @RequestBody ChangeQuantityRequest request) {
        specificationService.changeQuantity(AssemblyId.of(assemblyId), request);
    }

    @DeleteMapping("/{assemblyId}/rows")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRow(@PathVariable UUID assemblyId, @RequestBody RemoveRowRequest request) {
        specificationService.removeRow(AssemblyId.of(assemblyId), request);
    }

    @PostMapping("/{sourceAssemblyId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public AssemblyView copy(@PathVariable UUID sourceAssemblyId, @RequestBody CopySpecificationRequest request) {
        return specificationService.copy(AssemblyId.of(sourceAssemblyId), request);
    }
}
